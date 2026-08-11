/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.cowave.hub.home.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.lionsoul.ip2region.DataBlock;
import org.lionsoul.ip2region.DbConfig;
import org.lionsoul.ip2region.DbSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

/**
 *
 * 获取Ip地址
 *
 */
public class IpAddress {

    private static final Logger log = LoggerFactory.getLogger(IpAddress.class);

    /**
     * IP地址查询(太平洋IP地址查询) <a href="http://whois.pconline.com.cn/ipJson.jsp?ip=xxx.xxx.xxx.xxx&json=true">...</a>
     */
    private static final String WHOIS_URL = "http://whois.pconline.com.cn/ipJson.jsp";

    /**
     * <a href="https://ip.useragentinfo.com/json?ip=xxx.xxx.xxx.xxx">...</a>
     */
    private static final String IP_URL = "https://ip.useragentinfo.com/json";

    /**
     * <a href="https://api.vore.top/api/IPv4?v4=123.121.179.195">...</a>
     */
    private static final String VORE_URL = "https://api.vore.top/api/IPv4";

    // 未知地址
    private static final String UNKNOWN = "XX XX";

    private static DbConfig config;

    private static DbSearcher searcher;

    static {
        try {
            String dbPath = IpAddress.class.getResource("/ip2region.db").getPath();
            config = new DbConfig();
            searcher = new DbSearcher(config, dbPath);
        } catch (Exception e) {
            log.error("init ip region error", e);
        }
    }

    private static String getWhoIsAddressByIP(String ip) {
        // 内网不查询
        if (internalIp(ip)) {
            return "内网IP";
        }
        try {
            String rspStr = sendGet(WHOIS_URL, "ip=" + ip + "&json=true", "GBK");
            if (StringUtils.isEmpty(rspStr)) {
                log.error("获取地理位置异常 {}", ip);
                return UNKNOWN;
            }
            JSONObject obj = JSONObject.parseObject(rspStr);
            String region = obj.getString("pro");
            String city = StringUtils.isEmpty(obj.getString("region")) ? obj.getString("city") : obj.getString("region");
            return String.format("%s %s %s", region, city, "[太平洋IP提供]");
        } catch (Exception e) {
            log.error("获取地理位置异常", e);
        }
        return UNKNOWN;
    }

    private static String getRealAddressByIP(String ip) {
        // 内网不查询
        if (internalIp(ip)) {
            return "内网IP";
        }
        try {
            String rspStr = sendGet(IP_URL, "ip=" + ip + "&json=true", "UTF-8");
            if (StringUtils.isEmpty(rspStr)) {
                log.error("获取地理位置异常 {}", ip);
                return UNKNOWN;
            }
            JSONObject obj = JSONObject.parseObject(rspStr);
            if (!Objects.equals(obj.getInteger("code"), 200)) {
                log.error("获取地理位置异常 {}", obj);
                return UNKNOWN;
            }
            String province = obj.getString("province");
            String city = obj.getString("city");
            String area = obj.getString("area");
            return String.format("%s %s %s", province, city, area);
        } catch (Exception e) {
            log.error("获取地理位置异常", e);
        }
        return UNKNOWN;
    }

    private static String getVoreAddressByIP(String ip) {
        // 内网不查询
        if (internalIp(ip)) {
            return "内网IP";
        }
        try {
            String rspStr = sendGet(VORE_URL, "v4=" + ip, "UTF-8");
            if (StringUtils.isEmpty(rspStr)) {
                log.error("获取地理位置异常 {}", ip);
                return UNKNOWN;
            }
            JSONObject obj = JSONObject.parseObject(rspStr);
            if (!Objects.equals(obj.getInteger("code"), 200)) {
                log.error("获取地理位置异常 {}", obj);
                return UNKNOWN;
            }
            JSONObject ipData = obj.getJSONObject("ipdata");
            String province = ipData.getString("info1"); //
            String city = ipData.getString("info2");
            String area = ipData.getString("info3");
            return String.format("%s %s %s", province, city, area);
        } catch (Exception e) {
            log.error("获取地理位置异常", e);
        }
        return UNKNOWN;
    }

    public static String getRemoteAddressByIP(String ip) {
        if (internalIp(ip)) {
            return "内网IP";
        }
        String address = getVoreAddressByIP(ip);
        address = Objects.equals(UNKNOWN, address) ? getRealAddressByIP(ip) : address;
        address = Objects.equals(UNKNOWN, address) ? getWhoIsAddressByIP(ip) : address;
        address = Objects.equals(UNKNOWN, address) ? getRegionFormat(ip) : address;
        return address;
    }

    private static boolean internalIp(String ip) {
        byte[] addr = textToNumericFormatV4(ip);
        return internalIp(addr) || "127.0.0.1".equals(ip);
    }

    private static boolean internalIp(byte[] addr) {
        if (ArrayUtils.isEmpty(addr) || addr.length < 2) {
            return true;
        }
        final byte b0 = addr[0];
        final byte b1 = addr[1];
        // 10.x.x.x/8
        final byte SECTION_1 = 0x0A;
        // 172.16.x.x/12
        final byte SECTION_2 = (byte) 0xAC;
        final byte SECTION_3 = (byte) 0x10;
        final byte SECTION_4 = (byte) 0x1F;
        // 192.168.x.x/16
        final byte SECTION_5 = (byte) 0xC0;
        final byte SECTION_6 = (byte) 0xA8;
        switch (b0) {
            case SECTION_1:
                return true;
            case SECTION_2:
                if (b1 >= SECTION_3 && b1 <= SECTION_4) {
                    return true;
                }
            case SECTION_5:
                if (b1 == SECTION_6) {
                    return true;
                }
            default:
                return false;
        }
    }

    private static byte[] textToNumericFormatV4(String text) {
        if (text.length() == 0) {
            return null;
        }

        byte[] bytes = new byte[4];
        String[] elements = text.split("\\.", -1);
        long l;
        int i;
        switch (elements.length) {
            case 1 -> {
                l = Long.parseLong(elements[0]);
                if ((l < 0L) || (l > 4294967295L)) {
                    return null;
                }
                bytes[0] = (byte) (int) (l >> 24 & 0xFF);
                bytes[1] = (byte) (int) ((l & 0xFFFFFF) >> 16 & 0xFF);
                bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                bytes[3] = (byte) (int) (l & 0xFF);
            }
            case 2 -> {
                l = Integer.parseInt(elements[0]);
                if ((l < 0L) || (l > 255L)) {
                    return null;
                }
                bytes[0] = (byte) (int) (l & 0xFF);
                l = Integer.parseInt(elements[1]);
                if ((l < 0L) || (l > 16777215L)) {
                    return null;
                }
                bytes[1] = (byte) (int) (l >> 16 & 0xFF);
                bytes[2] = (byte) (int) ((l & 0xFFFF) >> 8 & 0xFF);
                bytes[3] = (byte) (int) (l & 0xFF);
            }
            case 3 -> {
                for (i = 0; i < 2; ++i) {
                    l = Integer.parseInt(elements[i]);
                    if ((l < 0L) || (l > 255L)) {
                        return null;
                    }
                    bytes[i] = (byte) (int) (l & 0xFF);
                }
                l = Integer.parseInt(elements[2]);
                if ((l < 0L) || (l > 65535L)) {
                    return null;
                }
                bytes[2] = (byte) (int) (l >> 8 & 0xFF);
                bytes[3] = (byte) (int) (l & 0xFF);
            }
            case 4 -> {
                for (i = 0; i < 4; ++i) {
                    l = Integer.parseInt(elements[i]);
                    if ((l < 0L) || (l > 255L)) {
                        return null;
                    }
                    bytes[i] = (byte) (int) (l & 0xFF);
                }
            }
            default -> {
                return null;
            }
        }
        return bytes;
    }

    private static String sendGet(String url, String param, String contentType) throws IOException {
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            String urlNameString = StringUtils.isNotBlank(param) ? url + "?" + param : url;
            URL realUrl = new URL(urlNameString);
            URLConnection connection = realUrl.openConnection();
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            connection.connect();
            in = new BufferedReader(new InputStreamReader(connection.getInputStream(), contentType));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        }finally {
            IOUtils.closeQuietly(in);
        }
        return result.toString();
    }

    private static String getRegionFormat(String ip) {
        try {
            String addr = getRegion(ip);
            String[] obj = addr.split("\\|");
            String province = obj[2];
            String city = obj[3];
            return String.format("%s %s %s", province, Objects.equals(city, "0") ? "" : city, "[ip2region提供]");
        } catch (Exception e) {
            log.error("查询ip地址库失败", e);
        }
        return UNKNOWN;
    }

    public static String getRegion(String ip) throws Exception {
        if (searcher == null || StringUtils.isEmpty(ip)) {
            return StringUtils.EMPTY;
        }
        int algorithm = DbSearcher.MEMORY_ALGORITYM;
        Method method = switch (algorithm) {
            case DbSearcher.BTREE_ALGORITHM -> searcher.getClass().getMethod("btreeSearch", String.class);
            case DbSearcher.BINARY_ALGORITHM -> searcher.getClass().getMethod("binarySearch", String.class);
            case DbSearcher.MEMORY_ALGORITYM -> searcher.getClass().getMethod("memorySearch", String.class);
            default -> null;
        };
        DataBlock dataBlock = (DataBlock) method.invoke(searcher, ip);
        return dataBlock.getRegion();
    }
}
