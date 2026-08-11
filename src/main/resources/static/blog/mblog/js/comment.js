$(function () {
    var pblogNickName = Core.getCookie("blog-userName");
    var pblogQQ = Core.getCookie("blog-qq");
    var pblogEmail = Core.getCookie("blog-email");
    if (pblogNickName !== "") {
        $("#user-name-content").show();
        $("#user-name").text(pblogNickName);
        $("#nickName").val(pblogNickName);
        $("#qq").val(pblogQQ);
        $("#email").val(pblogEmail);
    } else {
        $("#user-info").show();
    }
    var simplemde = new SimpleMDE({
        element: document.getElementById("comment-textarea"),
        toolbar: ['bold', 'heading','code', '|', 'quote','unordered-list','strikethrough','horizontal-rule','|','link','image','|','preview'],
        autoDownloadFontAwesome: false,
        placeholder: "支持markdown语法",
        renderingConfig: {
            markedOptions: {
                sanitize: true
            },
            codeSyntaxHighlighting: true
        },
        tabSize: 4,
        status: false
    });

    function init(pageNumber) {
        Core.postAjax("/comment/list", {
            "postId": postId,
            "page": (pageNumber == null ? 1 : pageNumber),
            "pageSize": 10
        }, function (resp) {
            var commentOne = "";
            var data = resp.data;
            if (data.total === 0) {
                commentOne += '<div class="no-comment">暂无评论，快来占领宝座</div>';
                $("#comment-ul").html(commentOne);
            } else {
                $.each(data.list, function (index, value) {
                    commentOne +=
                        '<li>' +
                        '	<div class="comment-body" id="comment-' + value.id + '">' +
                        '		<div class="comment-user-img">' +
                        '			<img src="' + (value.avatar||'/img/unknown.jpg') + '" onerror="this.src=\'/img/unknown.jpg\'" />' +
                        '		</div>' +
                        '		<div class="comment-info">' +
                        '			<div class="comment-top">' +
                        '				<span class="comment-nickName">' +
                        '					<a href="javascript:void(0)">' + value.nickName + '</a>' +
                        '				<span class="comment-time" >' + value.createTime +
                        '				</span>' +
                        '                   <span style="padding: 3px 5px; color: #fd7e14; background-color: #fff4e6; border-radius: 5px;"> '+value.ipAddr +'</span>'
                        '				</span>' +
                        '			</div>' +
                        '           <div class="comment-content">';
                    commentOne += value.content +
                        '			</div>' +
                        '			<div class="comment-footer">' +
                        '				<span class="reply mr-5" reply-id="' + value.id + '">回复</span>' +
                        '				<span class="cancel-reply mr-5" style="display: none;">取消回复</span>' +
                        '			</div>' +
                        '		</div>' +
                        '	</div> '
                    // 回复评论start
                    if(value.replyComments !=null && value.replyComments.length > 0 ) {
                        commentOne +=
                            '<div class="social-footer">'
                        for (reply of (value.replyComments)) {
                            commentOne +=
                                '  <div class="social-comment">' +
                                '       <a href="" class="pull-left">' +
                                '           <img alt="image" src="' + (reply.avatar||'/img/unknown.jpg') + '" onerror="this.src=\'/img/unknown.jpg\'">' +
                                '       </a>' +
                                '       <div class="media-body">' +
                                '           <a href="#">' + reply.nickName +
                                '                   </a> <small style="padding: 3px 5px; color: #fd7e14; background-color: #fff4e6; border-radius: 5px;">' +reply.ipAddr+ '</small>' +
                                ' <small >'+ reply.createTime +'</small> '+ reply.content +
                                '       </div>' +
                                '    </div> </br>' ;
                        }
                        '</div>'
                    }
                    // 回复评论end
                    '</li>';
                })
                $("#comment-more").remove();

                if (((data.currentPage * data.currentSize) < data.total)) {
                    commentOne += '<div id="comment-more" data-page="' + (data.currentPage + 1) + '" class="comment-more">加载更多</div>'
                }
                $("#comment-ul").html(commentOne);
                /*加载更多*/
                $("#comment-more").click(function () {
                    init($(this).attr("data-page"));
                })
                /*link至评论*/
                $(".comment-link").click(function () {
                    var commentLinkId = $(this).attr("data-link");
                    $("html,body").animate({
                        scrollTop: $("#" + commentLinkId).offset().top - 55
                    }, {duration: 300, easing: "swing"})
                })

                $(".reply").click(function () {
                    var replyId = $(this).attr("reply-id");
                    if ($("#reply-comment-form").length > 0) {
                        $replyForm = $("#reply-comment-form");
                        $("#reply-comment-form").remove();
                        $(this).parent().after($replyForm);
                        $("#reply-comment-form").show();
                        $(".reply[style='display: none;']").next().hide();
                        $(".reply[style='display: none;']").show();
                        $("#replyId").val(replyId);
                    } else {
                        var replyForm =
                            '<form id="reply-comment-form" class="form-horizontal mt-10">' +
                            '   <input name="postId" type="hidden" value="' + postId + '"  />' +
                            '   <input id="replyId" name="pId" type="hidden" value="' + replyId + '"  />' +
                            '   <div id="user-info" class="user-name-content" style="display: none">登录后回复</div> ' +
                            '   <div class="form-group">' +
                            '       <div class="col-xs-12">' +
                            '           <textarea name="content" id="reply-comment-textarea"></textarea>' +
                            '       </div>' +
                            '   </div>' +
                            '   <div>' +
                            '   <button id="submitReplyCommentBtn" type="button" class="btn btn-primary">回复</button>' +
                            '   </div>' +
                            '</form>'
                        $(this).parent().after(replyForm);
                        var replySimplemde = new SimpleMDE({
                            element: document.getElementById("reply-comment-textarea"),
                            toolbar: [],
                            autoDownloadFontAwesome: false,
                            placeholder: "说点什么好呢",
                            renderingConfig: {
                                codeSyntaxHighlighting: true
                            },
                            tabSize: 4,
                            status: false
                        });
                    }
                    $(this).hide();
                    $(this).next().show();
                    $("#submitReplyCommentBtn").on('click', function () {
                        if (pblogNickName === "") {
                            layer.msg("请先登录!!")
                            return;
                        } else if (replySimplemde.value() === "") {
                            layer.msg("说点什么吧")
                            return;
                        }
                        $("#reply-comment-textarea").val(filterXSS(replySimplemde.markdown(replySimplemde.value())));
                        Core.postAjax("/comment/message", $("#reply-comment-form").serialize(), function (data) {
                            if (data.code === "200") {
                                simplemde.value('');
                                init();
                            }else{
                                layer.msg(data.msg)
                            }
                        })
                    })
                })

                $(".cancel-reply").click(function () {
                    $("#reply-comment-form").hide();
                    $(this).hide();
                    $(this).prev().show();
                })

                $(".comment-support").click(function () {
                    $thisLove = $(this);
                    Core.postAjax("/blog/api/love", {"bizId": $(this).attr("biz-id"), "bizType": 2}, function (data) {
                        if (data.status === 200) {
                            $thisLove.text(parseInt($thisLove.text()) + 1);
                        }
                    });
                })
            }

        })
    }

    init();

    /*提交评论*/
    $("#submitCommentBtn").click(function () {
        if (pblogNickName === "") {
            layer.msg("请先登录!!")
            return;
        } else if (simplemde.value() === "") {
            layer.msg("说点什么吧")
            return;
        }
        $("#comment-textarea").val(filterXSS(simplemde.markdown(simplemde.value())));
        Core.postAjax("/comment/message", $("#comment-form").serialize(), function (data) {
            if (data.code === "200") {
                simplemde.value('');
                init();
            }else{
                layer.msg(data.msg)
            }
        })
    })
})
