/// <reference path="jquery-1.8.0.min.js" />
//.loadingPage_bg { background: none repeat scroll 0 0 #fff; display: block; height: 100%; left: 0; /*:rgba(0,0,0,0.5);*/ opacity: 0.1; filter: alpha(opacity=50); position: absolute; top: 0; width: 100%; z-index: 110; }
//#loadingPage { display: block; font-weight: bold; font-size: 12px; color: #595959; height: 28px; left: 50%; line-height: 27px; margin-left: -74px; margin-top: -14px; padding: 10px 10px 10px 50px; position: absolute; text-align: left; top: 50%; width: 148px; z-index: 111; background: url(img/loading.gif) no-repeat scroll 12px center #FFFFFF; border: 2px solid #86A5AD; }


var CommonPerson = {};
CommonPerson.Base = {};
CommonPerson.Base.LoadingPic = {
    operation: {
        timeTest: null,
        loadingCount: 0,
        loadingImgUrl: "img/loading.gif",
        loadingImgHeight: 24,
        loadingImgWidth: 24
    },

    FullScreenShow: function (msg) {
        if (msg === undefined) {
            msg = "Loading data, please wait...";
        }

        if ($("#div_loadingImg").length == 0) {
            $("body").append("<div id='div_loadingImg'></div>");
        }
        if (this.operation.loadingCount < 1) {
            this.operation.timeTest = setTimeout(function () {
                $("#div_loadingImg").append("<div id='loadingPage_bg' class='loadingPage_bg1'></div><div id='loadingPage'>" + msg + "</div>");
                $("#loadingPage_bg").height($(top.window.document).height()).width($(top.window.document).width());
            }, 100);
        }
        this.operation.loadingCount += 1;
    },

    FullScreenHide: function () {
        this.operation.loadingCount -= 1;
        if (this.operation.loadingCount <= 0) {
            clearTimeout(this.operation.timeTest);
            $("#div_loadingImg").empty();
            $("#div_loadingImg").remove();
            this.operation.loadingCount = 0;
        }
    },

    PartShow: function (parentContainerID, url, msg) {
        $("#" + parentContainerID.replace("#", "").replace(".", "") + "_loadingImg").remove();
        var imgUrl = '';
        if (url) {
            imgUrl = url;
        } else {
            imgUrl = this.operation.loadingImgUrl;
        }

        if (msg === undefined) {
            msg = "Loading...";
        }

        var htmlText = ' <div id="' + parentContainerID + '_loadingImg" class="loadingPage_bg"><div style="display: block; font-weight: bold; font-size: 12px; color: #595959; height: 28px; left: 50%; line-height: 27px; padding: 10px 10px 10px 50px; width: 240px; z-index: 111; background: url(img/loading.gif) no-repeat scroll 12px center #FFFFFF; border: 2px solid #86A5AD;">' + msg + '</div></div>'
        $("#" + parentContainerID).append(htmlText);
    },

    PartHide: function (parentContainerID) {
        $("#" + parentContainerID.replace("#", "").replace(".", "") + "_loadingImg").remove();
    },

    PartOnlyImgShow: function (parentContainerID, url) {
        $("#" + parentContainerID.replace("#", "").replace(".", "") + "_zhezhao").remove();
        var parentContainer = $("#" + parentContainerID);
        var imgTop = parentContainer.height() / 2 - this.operation.loadingImgHeight / 2;
        var imgLeft = parentContainer.width() / 2 - this.operation.loadingImgWidth / 2;

        var imgUrl = '';
        if (url) {
            imgUrl = url;
        } else {
            imgUrl = this.operation.loadingImgUrl;
        }

        var htmlText = '<div id="' + parentContainerID.replace("#", "").replace(".", "") + '_zhezhao" class="loadingPage_bg" style="margin:10px;display:block;position: absolute; width:' + parentContainer.width() + 'px; border: 1px solid #D6E9F1; z-index:1002;"><img style="position: absolute; top:' + imgTop + 'px; left:' + imgLeft + 'px; border: 1px solid #D6E9F1;" src="' + imgUrl + '"/> </div>'
        $("body").append(htmlText);

        var zhezhao = $("#" + parentContainerID.replace("#", "").replace(".", "") + "_zhezhao");
        zhezhao.css("top", parentContainer.offset().top + "px");
        zhezhao.css("left", parentContainer.offset().left + "px");
        zhezhao.css("width", parentContainer.width() + "px");
    },

    PartOnlyImgHide: function (parentContainerID) {
        $("#" + parentContainerID.replace("#", "").replace(".", "") + "_zhezhao").remove();
    }

}