<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page errorPage = "/VersaStack-web/errorPage.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="user" class="web.beans.userBeans" scope="session" />
<jsp:setProperty name="user" property="*" />  
<jsp:useBean id="serv" class="web.beans.serviceBeans" scope="page" />
<jsp:setProperty name="serv" property="*" />  
<c:if test="${user.loggedIn == false}">
    <c:redirect url="/index.jsp" />
</c:if>
<!DOCTYPE html>
<html >    
    <head>   
        <meta charset="UTF-8">
        <title>Template Service</title>
        <script src="/VersaStack-web/js/jquery/jquery.js"></script>
        <script src="/VersaStack-web/js/bootstrap.js"></script>
        <script src="/VersaStack-web/js/nexus.js"></script>

        <link rel="stylesheet" href="/VersaStack-web/css/animate.min.css">
        <link rel="stylesheet" href="/VersaStack-web/css/font-awesome.min.css">
        <link rel='stylesheet prefetch' href='http://fonts.googleapis.com/css?family=Roboto:400,100,400italic,700italic,700'>
        <link rel="stylesheet" href="/VersaStack-web/css/bootstrap.css">
        <link rel="stylesheet" href="/VersaStack-web/css/style.css">
        <link rel="stylesheet" href="/VersaStack-web/css/driver.css">
        <style>
            .hover_div {
                    position: fixed;
                    visibility: hidden;
                    background-color: rgba(255,255,255,.85);
                    color: #404040;
                    border: 1px solid #999;
                    padding: 7px;
            }

            .details_viz {
                width:100%;
                height:100%;
            }
            
            .inactive_details_viz td{
                border-top: 0px;
                background-color: #777;
            }
            
            .inactive_details_viz th {
                color: #ccc;
            }
            
        </style>
         <script>
            //Based off http://dojotoolkit.org/documentation/tutorials/1.10/dojo_config/ recommendations
            dojoConfig = {
                has: {
                    "dojo-firebug": true,
                    "dojo-debug-messages": true
                },
                async: true,
                parseOnLoad: true,
                packages: [
                    {
                        name: "d3",
                        location: "//d3js.org/",
                        main: "d3.v3"
                    },
                    {
                        name: "local",
                        location: "/VersaStack-web/js/"
                    }
                ]
            };
        </script>
        <script src="//ajax.googleapis.com/ajax/libs/dojo/1.10.0/dojo/dojo.js"></script>
       
        <script type="text/javascript">
            var settings = {
                ZOOM_FACTOR: .04,
                ZOOM_MIN: .8,
                INIT_ZOOM: 2  //The initial zoom factor effects the preciosion in which we can specify the highlighting effect
                        //However, it also seems to effect the error in zooming
            };
            var ModelConstructor;
            var model;
            var layout;
            var render;
            var d3;
            var utils;
            var DropDownTree;

            var outputApi;

            function onload() {
                require(["local/versastack/topology/model",
                    "local/versastack/topology/layout",
                    "local/versastack/topology/render",
                    "local/d3",
                    "local/versastack/utils",
                    "local/versastack/topology/DropDownTree",
                    "local/versastack/topology/ContextMenu"                ],
                        function (m, l, r, d3_, utils_, tree, c) {
//                          $.ajax({
//                                   crossDomain: true,
//                                   type: "GET",
//                                   url: "/VersaStack-web/restapi/service/ready",
//                                   dataType: "text", 
//
//                                   success: function(data,  textStatus,  jqXHR ) {
//                                       if (data === "true")  {
                                          //alert(textStatus);
                                            layout = l;
                                            render = r;
                                            d3 = d3_;
                                            utils = utils_;
                                            map_ = utils.map_;
                                            bsShowFadingMessage = utils.bsShowFadingMessage;
                                            // possibly pass in map here later for all possible dialogs 
                                            
                                            //outputApi = new outputApi_(render.API, null, "viz");
                                            //outputApi2 = new outputApi_(render.API, contextMenu, "viz2");

                                            ModelConstructor = m;
                                            model = new ModelConstructor();
                                            model.init(1, renderModels, null);
                                            //model.init(1, drawGraph.bind(undefined, outputApi, model), null);
                                            //model2 = new ModelConstructor();
                                            //model2.init(1, drawGraph.bind(undefined, outputApi2, model2), null);     
                                            //renderModels();
                                           console.log("after model.");
//                                       } else {
//                                           //displayError("Visualization Unavailable", d3_);
//                                      }
//                                   },
//
//                                   error: function(jqXHR, textStatus, errorThrown ) {
//                                        console.log("Debugging: timeout at start..");
//                                       // displayError("Visualization Unavailable", d3_);
//                                     //alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
//                                   }
//                            }); 
                        });



                //$("#loadingPanel").addClass("hide");
                //$("#hoverdiv_viz").removeClass("hide");
                //$("#hoverdivviz2").removeClass("hide");

                //$("#viz").attr("class", "");
                //$("#viz2").attr("class", "");
                //alert("i'm here. ")
            }
            
            function displayError(error, d3_obj) {
               d3_obj.select("#viz").append("text")
                       .attr("x", $(window).width() / 4)
                       .attr("y", $(window).height() / 2 )
                       .attr("fill", "black")
                       .attr("font-size", "80px")
                       .text(error);

            }
            
           
            function drawGraph(outputApi, model) {
                var width =  $("#va_viz").closest("td").width();//document.documentElement.clientWidth / settings.INIT_ZOOM;
                var height = $( "#va_viz").closest("td").height(); //document.documentElement.clientHeight / settings.INIT_ZOOM;
                //TODO, figure out why we need to call this twice
                //If we do not, the layout does to converge as nicely, even if we double the number of iterations
                layout.doLayout(model, null, width, height);
                layout.doLayout(model, null, width, height);
                outputApi.setZoom(.8);
                render.doRender(outputApi, model, false);
//                animStart(30);
            }
            
            function showDiactivatedViz(viz_id) {
               var viz_container =  $("#" + viz_id).closest("td");
               var index = viz_container.index();
               //viz_container.addClass("inactive_details_viz");
               viz_container.css({
                 "border-top" : "0px",
                 "background-color" : "#777"
               });
               //var viz_table = viz_container.closest("table");
               //viz_table.find("th:nth-child(" + index + ")").css( "color", "#ccc");
            }
            
            function renderModels() {
                var UUID = prompt("Enter UUID", "1a33d50a-383c-4252-a67c-28e4d3fea0e6");

                $.ajax({
                    crossDomain: true,
                    type: "GET",
                    url: "/VersaStack-web/restapi/app/service/lastverify/" + UUID,
                    dataType: "json", 

                    success: function(data,  textStatus,  jqXHR ) {
                         if (data.verified_addition && data.verified_addition !==  '{ }') {
                            var vaObj = JSON.parse(data.verified_addition);
                            var vaModel = new ModelConstructor();
                            vaModel.initWithMap(vaObj, model);
                            var outputApi = new outputApi_(render.API, null, "va_viz");
                            drawGraph(outputApi, vaModel);
                         }  else {
                             showDiactivatedViz("va_viz");
                         }
                        
                        if (data.verified_reduction && data.verified_reduction !==  '{ }') {
                            var vrObj = JSON.parse(data.verified_reduction);
                            var vrModel = new ModelConstructor();
                            vrModel.initWithMap(vrObj, model);
                            var outputApi2 = new outputApi_(render.API, null, "vr_viz");
                            drawGraph(outputApi2, vrModel);                       
                        } else {
                            showDiactivatedViz("vr_viz");
                        }
                        
                        if (data.unverified_addition && data.unverified_addition !==  '{ }') {
                            var uaObj = JSON.parse(data.unverified_addition);
                            var uaModel = new ModelConstructor();
                            uaModel.initWithMap(uaObj, model);
                            var outputApi3 = new outputApi_(render.API, null, "ua_viz");
                            drawGraph(outputApi3, uaModel);                        
                        } else {
                            showDiactivatedViz("ua_viz");
                        }
                        
                        if (data.unverified_reduction && data.unverified_reduction !==  '{ }'){
                            var urObj = JSON.parse(data.unverified_reduction);
                            var urModel = new ModelConstructor();
                            urModel.initWithMap(urObj, model);
                            var outputApi4 = new outputApi_(render.API, null, "ur_viz");
                            drawGraph(outputApi4, urModel);                        
                        } else {
                            showDiactivatedViz("ur_viz");
                        }
                        
                    },

                    error: function(jqXHR, textStatus, errorThrown ) {
                        //alert("Error getting status.");
                       // alert("textStatus: " + textStatus + " errorThrown: " + errorThrown);
                       alert("not found");
                    }
               });                     
                
            }
            function outputApi_(renderAPI, contextMenu, svg) {
                var that = this;
                this.renderApi = renderAPI;
                this.contextMenu = contextMenu;
                this.svgContainerName = svg;
                
                this.getSvgContainer = function () {
                    return d3.select("#" + this.svgContainerName);
                };

                
                this.getDisplayTree = function () {
                    return null;
                };

                this.setDisplayName = function (name) {
                    //document.getElementById("displayName").innerText = name;
                };

                var zoomFactor = settings.INIT_ZOOM;
                var offsetX = 0, offsetY = 0;
                this.zoom = function (amount, mouseX, mouseY) {
                    /*
                     * There seems to be some error (rounding?) when we zoom far out
                     * 
                     * In addition to zooming, we also translate the image so that the point under the cursor appears stationary.
                     * To understand this conversion, there are two coordinate systems to consdier:
                     *      the svg coordinate system (in which all svg objects are stationary throughout the translation), and
                     *      the mouse coordinate system, that corresponds to what we see
                     *      
                     *       The conversion (along a single axis) between these two systems is given by:
                     *       X_mouse=zoomFactor*(X_svg + offsetX)
                     *       To find the new offset, after increasing zoomFactor by zoomDelta, we solve:
                     *         zoomFactor*(X_svg + offsetX)=(zoomFactor+zoomDelta)*(X_svg + (offsetX+doX))
                     *        for doX.
                     *        the new offset is now offsetX+doX
                     *        
                     */
                    var zoomFactorNew = zoomFactor * (1 + amount);
                    var zoomDelta = zoomFactorNew - zoomFactor;
                    if (zoomFactorNew < settings.ZOOM_MIN) {
                        return;
                    }
                    zoomFactor = zoomFactorNew;
                    //Translate so that the point under the mouse does not move
                    //get the svg coordinate of the mouse
                    var svgCoords = this.convertCoords(mouseX, mouseY);
                    offsetX -= zoomDelta * (svgCoords.x + offsetX) / zoomFactor;
                    offsetY -= zoomDelta * (svgCoords.y + offsetY) / zoomFactor;
                    this._updateTransform();
                };
                this.getZoom = function () {
                    return zoomFactor;
                };
                this.scroll = function (dx, dy) {
                    offsetX += dx / zoomFactor;
                    offsetY += dy / zoomFactor;
                    this._updateTransform();
                };

                this._updateTransform = function () {
                    d3.select("#transform" + "_" + this.svgContainerName).
                            attr("transform", "scale(" + zoomFactor + ")translate(" + offsetX + "," + offsetY + ")");
                };
                this._updateTransform();
                //Return the svg coordinates of the point under the given mouse coords
                //The mouseCoords should be from event.offsetX (offsetY)
                this.convertCoords = function (mouseX, mouseY) {
                    var ans = {};
                    ans.x = mouseX / zoomFactor - offsetX;
                    ans.y = mouseY / zoomFactor - offsetY;
                    return ans;
                };

                this.setHoverText = function (str) {
                    document.getElementById("hoverdiv" + "_" + this.svgContainerName).innerText = str;
                };
                this.setHoverLocation = function (x, y) {
                    document.getElementById("hoverdiv" + "_" + this.svgContainerName).style.left = x + "px";
                    document.getElementById("hoverdiv" + "_" + this.svgContainerName).style.top = y + 10 + "px";
                };
                this.setHoverVisible = function (vis) {
                    document.getElementById("hoverdiv" + "_" + this.svgContainerName).style.visibility = vis ? "visible" : "hidden";
                };

                this.resetZoom = function () {   // @
                    zoomFactor = settings.INIT_ZOOM;
                    offsetX = 0;
                    offsetY = 0;                    
                    this._updateTransform();
                };
                this.setZoom = function(zoom) {
                    zoomFactor = zoom;
                    this._updateTransform();
                };
                var svg = document.getElementById(this.svgContainerName);
                svg.addEventListener("mousewheel", function (e) {
                    e.preventDefault();
                    //The OSX trackpad seems to produce scrolls two orders of magnitude large when using pinch to zoom,
                    //so we ignore the magnitude entirely
                    that.zoom(Math.sign(e.wheelDeltaY) * settings.ZOOM_FACTOR, e.offsetX, e.offsetY);
                    return false;
                }, false);

                //Interface to (de)select elements for interaction with the form
                //If the provided element is currently being used in the form, remove it from the form,
                //Otherwise use it to help poupulate the form.
                //The precises meaning of this depends on the element and form type
                this.formSelect = function (n) {
                    if (!n.getType) {
                        console.log("Trying to form select object without getType function");
                        return;
                    }
                    switch (n.getType()) {
                        case "Subnet":
                            var forms = document.getElementsByName("subnets");
                            if (forms.length === 0) {
                                break;
                            }
                            if (forms.length > 1) {
                                console.log("More than 1 subnets forms");
                                break;
                            }
                            var form = forms[0];
                            var subnetStrs = [];
                            var toToggle = n.getNameBrief();
                            var add = true;
                            //If the form is already empty, we would add an extranous empty line
                            if (form.value.trim() !== "") {
                                map_(form.value.split('\n'), function (subnetStr) {
                                    if (subnetStr.trim() === toToggle) {
                                        add = false;
                                    } else {
                                        subnetStrs.push(subnetStr);
                                    }
                                    ;
                                });
                            }
                            if (add) {
                                subnetStrs.push(toToggle);
                            }
                            form.value = subnetStrs.join('\n');
                            break;
                        default:
                            console.log("Unhandled type in formSelect: " + n.getType());
                    }
                };

                //Panning
                //d3 does not seem to provide a way for us to avoid capturing mouse events that are handled by nodes
                //This means that our panning code will be called even when the user is only trying to move a node
                //as a work around, we provide a way to enable/disable panning
                var panningEnabled = true;
                var isPanning = false;
                var moved = false;
                this.enablePanning = function () {
                    panningEnabled = true;
                };
                this.disablePanning = function () {
                    panningEnabled = false;
                };
                svg.addEventListener("mousedown", function (e) {
                    moved = false;
                    isPanning = true;
                });
                svg.addEventListener("mousemove", function (e) {
                    // && (e.which ==== 1) stops d3 bug of dragging to enable on context menu 
                    if (isPanning && panningEnabled && (e.which === 1) )   {
                        moved = true;
                        that.scroll(e.movementX, e.movementY);
                    }
                });
                svg.addEventListener("mouseup", function (e) {
                    isPanning = false;
                    if (moved) {
                        e.preventDefault();
                    }
                });
            }
        </script>        
    </head>

    <sql:setDataSource var="rains_conn" driver="com.mysql.jdbc.Driver"
                       url="jdbc:mysql://localhost:3306/rainsdb"
                       user="root"  password="root"/>

    <body onload="onload()">
        <!-- NAV BAR -->
        <div id="nav">
        </div>
        <!-- SIDE BAR -->
        <div id="sidebar">            
        </div>
        <!-- MAIN PANEL -->
        <div id="main-pane">
                 <table class="management-table">
                    <thead>
                        <tr>
                            <th>Verified Addition</th>
                            <th>Verified Reduction</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>
                                    <div class="hover_div" id="hoverdiv_va_viz"></div>        

                                    <svg  class ="details_viz" id="va_viz">
                                    <defs>
                                    <!--When we highlight topologies without specifiyng a width and length, the get clipped
                                    The x,y offset are so we avoid clipping the top and left edges.
                                    I simply added zeroes until this worked in all cases I tested (the extreme case being a vertical or horizontal topology
                                    We could highlight the element entirly with this filter by using feBlend instead of feComposite,
                                    However, then (in the case of topologies) the outline would be stuck with the same opacity as the entire element, which is not desireable
                                    Instead, we will simply duplicate the selected element, and show its outline as an overlay
                                    -->
                                <filter id="outline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
                                    <feMorphology operator="dilate" radius="1"/>
                                    <feColorMatrix result="a" type="matrix"
                                                   values="0 0 0 0 .7
                                                   0 0 0 0 1
                                                   0 0 0 0 0
                                                   0 0 0 1 0" />
                                    <feComposite operator="out" in="a" in2="SourceGraphic"/>
                                </filter>

                            <filter id="serviceHighlightOutline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%" >
                               <feFlood flood-color="#66ff66" result="base" />
                               <feMorphology result="bigger" in="SourceGraphic" operator="dilate" radius="1"/>
                               <feColorMatrix result="mask" in="bigger" type="matrix"
                                  values="0 0 0 0 0
                                          0 0 0 0 0
                                          0 0 0 0 0
                                          0 0 0 1 0" />
                               <feComposite result="drop" in="base" in2="mask" operator="in" />
                               <feBlend in="SourceGraphic" in2="drop" mode="normal" />
                            </filter>

                                <filter id="subnetHighlight" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
                                    <feMorphology operator="dilate" radius="1"/>
                                    <feColorMatrix result="a" type="matrix"
                                                   values="0 0 0 0 0
                                                   0 0 0 0 .8
                                                   0 0 0 0 .3
                                                   0 0 0 1 0" />
                                    <feComposite operator="out" in="a" in2="SourceGraphic"/>
                                </filter>
                                <filter id="ghost" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <feColorMatrix type="saturate" values=".2"/>
                                </filter>
                                </defs>
                                <!--We nest a g in here because the svg tag itself cannot do transforms
                                    we separate topologies, edges, and nodes to create an explicit z-order
                                -->
                                <g id="transform_va_viz">
                                <g id="topology_va_viz"/>
                                <g id="edge1_va_viz"/>
                                <g id="anchor_va_viz"/>
                                <g id="node_va_viz"/>
                                <g id="dialogBox_va_viz"/>
                                <g id="volumeDialogBox_va_viz"/>
                                <g id="switchPopup_va_viz"/>
                                <g id="parentPort_va_viz"/>
                                <g id="edge2_va_viz" />
                                <g id="port_va_viz"/>
                                <g id="volume_va_viz"/>

                                </g>
                                </svg>
                                
                            </td>
                            <td>
                                    <div class="hover_div" id="hoverdiv_vr_viz"></div>        

                                    <svg class ="details_viz" id="vr_viz">
                                    <defs>
                                    <!--When we highlight topologies without specifiyng a width and length, the get clipped
                                    The x,y offset are so we avoid clipping the top and left edges.
                                    I simply added zeroes until this worked in all cases I tested (the extreme case being a vertical or horizontal topology
                                    We could highlight the element entirly with this filter by using feBlend instead of feComposite,
                                    However, then (in the case of topologies) the outline would be stuck with the same opacity as the entire element, which is not desireable
                                    Instead, we will simply duplicate the selected element, and show its outline as an overlay
                                    -->
                                <filter id="outline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
                                    <feMorphology operator="dilate" radius="1"/>
                                    <feColorMatrix result="a" type="matrix"
                                                   values="0 0 0 0 .7
                                                   0 0 0 0 1
                                                   0 0 0 0 0
                                                   0 0 0 1 0" />
                                    <feComposite operator="out" in="a" in2="SourceGraphic"/>
                                </filter>

                            <filter id="serviceHighlightOutline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%" >
                               <feFlood flood-color="#66ff66" result="base" />
                               <feMorphology result="bigger" in="SourceGraphic" operator="dilate" radius="1"/>
                               <feColorMatrix result="mask" in="bigger" type="matrix"
                                  values="0 0 0 0 0
                                          0 0 0 0 0
                                          0 0 0 0 0
                                          0 0 0 1 0" />
                               <feComposite result="drop" in="base" in2="mask" operator="in" />
                               <feBlend in="SourceGraphic" in2="drop" mode="normal" />
                            </filter>

                                <filter id="subnetHighlight" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
                                    <feMorphology operator="dilate" radius="1"/>
                                    <feColorMatrix result="a" type="matrix"
                                                   values="0 0 0 0 0
                                                   0 0 0 0 .8
                                                   0 0 0 0 .3
                                                   0 0 0 1 0" />
                                    <feComposite operator="out" in="a" in2="SourceGraphic"/>
                                </filter>
                                <filter id="ghost" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <feColorMatrix type="saturate" values=".2"/>
                                </filter>
                                </defs>
                                <!--We nest a g in here because the svg tag itself cannot do transforms
                                    we separate topologies, edges, and nodes to create an explicit z-order
                                -->
                                <g id="transform_vr_viz">
                                <g id="topology_vr_viz"/>
                                <g id="edge1_vr_viz"/>
                                <g id="anchor_vr_viz"/>
                                <g id="node_vr_viz"/>
                                <g id="dialogBox_vr_viz"/>
                                <g id="volumeDialogBox_vr_viz"/>
                                <g id="switchPopup_vr_viz"/>
                                <g id="parentPort_vr_viz"/>
                                <g id="edge2_vr_viz" />
                                <g id="port_vr_viz"/>
                                <g id="volume_vr_viz"/>

                                </g>
                                </svg>                                
                            </td>
                        </tr>
                    </tbody>
                </table>

                 <table class="management-table">
                    <thead>
                        <tr>
                            <th>Unverified Addition</th>
                            <th>Unverified Reduction</th>
                        </tr>
                    </thead>
                    <tbody>
                        <td>
                                    <div class="hover_div" id="hoverdiv_ua_viz"></div>        

                                    <svg  class ="details_viz" id="ua_viz">
                                    <defs>
                                    <!--When we highlight topologies without specifiyng a width and length, the get clipped
                                    The x,y offset are so we avoid clipping the top and left edges.
                                    I simply added zeroes until this worked in all cases I tested (the extreme case being a vertical or horizontal topology
                                    We could highlight the element entirly with this filter by using feBlend instead of feComposite,
                                    However, then (in the case of topologies) the outline would be stuck with the same opacity as the entire element, which is not desireable
                                    Instead, we will simply duplicate the selected element, and show its outline as an overlay
                                    -->
                                <filter id="outline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
                                    <feMorphology operator="dilate" radius="1"/>
                                    <feColorMatrix result="a" type="matrix"
                                                   values="0 0 0 0 .7
                                                   0 0 0 0 1
                                                   0 0 0 0 0
                                                   0 0 0 1 0" />
                                    <feComposite operator="out" in="a" in2="SourceGraphic"/>
                                </filter>

                            <filter id="serviceHighlightOutline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%" >
                               <feFlood flood-color="#66ff66" result="base" />
                               <feMorphology result="bigger" in="SourceGraphic" operator="dilate" radius="1"/>
                               <feColorMatrix result="mask" in="bigger" type="matrix"
                                  values="0 0 0 0 0
                                          0 0 0 0 0
                                          0 0 0 0 0
                                          0 0 0 1 0" />
                               <feComposite result="drop" in="base" in2="mask" operator="in" />
                               <feBlend in="SourceGraphic" in2="drop" mode="normal" />
                            </filter>

                                <filter id="subnetHighlight" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
                                    <feMorphology operator="dilate" radius="1"/>
                                    <feColorMatrix result="a" type="matrix"
                                                   values="0 0 0 0 0
                                                   0 0 0 0 .8
                                                   0 0 0 0 .3
                                                   0 0 0 1 0" />
                                    <feComposite operator="out" in="a" in2="SourceGraphic"/>
                                </filter>
                                <filter id="ghost" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <feColorMatrix type="saturate" values=".2"/>
                                </filter>
                                </defs>
                                <!--We nest a g in here because the svg tag itself cannot do transforms
                                    we separate topologies, edges, and nodes to create an explicit z-order
                                -->
                                <g id="transform_ua_viz">
                                <g id="topology_ua_viz"/>
                                <g id="edge1_ua_viz"/>
                                <g id="anchor_ua_viz"/>
                                <g id="node_ua_viz"/>
                                <g id="dialogBox_ua_viz"/>
                                <g id="volumeDialogBox_ua_viz"/>
                                <g id="switchPopup_ua_viz"/>
                                <g id="parentPort_ua_viz"/>
                                <g id="edge2_ua_viz" />
                                <g id="port_ua_viz"/>
                                <g id="volume_ua_viz"/>

                                </g>
                                </svg>                            
                        </td>

                        <td>
                                    <div class="hover_div" id="hoverdiv_ur_viz"></div>        

                                    <svg class ="details_viz" id="ur_viz">
                                    <defs>
                                    <!--When we highlight topologies without specifiyng a width and length, the get clipped
                                    The x,y offset are so we avoid clipping the top and left edges.
                                    I simply added zeroes until this worked in all cases I tested (the extreme case being a vertical or horizontal topology
                                    We could highlight the element entirly with this filter by using feBlend instead of feComposite,
                                    However, then (in the case of topologies) the outline would be stuck with the same opacity as the entire element, which is not desireable
                                    Instead, we will simply duplicate the selected element, and show its outline as an overlay
                                    -->
                                <filter id="outline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
                                    <feMorphology operator="dilate" radius="1"/>
                                    <feColorMatrix result="a" type="matrix"
                                                   values="0 0 0 0 .7
                                                   0 0 0 0 1
                                                   0 0 0 0 0
                                                   0 0 0 1 0" />
                                    <feComposite operator="out" in="a" in2="SourceGraphic"/>
                                </filter>

                            <filter id="serviceHighlightOutline" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%" >
                               <feFlood flood-color="#66ff66" result="base" />
                               <feMorphology result="bigger" in="SourceGraphic" operator="dilate" radius="1"/>
                               <feColorMatrix result="mask" in="bigger" type="matrix"
                                  values="0 0 0 0 0
                                          0 0 0 0 0
                                          0 0 0 0 0
                                          0 0 0 1 0" />
                               <feComposite result="drop" in="base" in2="mask" operator="in" />
                               <feBlend in="SourceGraphic" in2="drop" mode="normal" />
                            </filter>

                                <filter id="subnetHighlight" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <!--https://msdn.microsoft.com/en-us/library/hh773213(v=vs.85).aspx-->
                                    <feMorphology operator="dilate" radius="1"/>
                                    <feColorMatrix result="a" type="matrix"
                                                   values="0 0 0 0 0
                                                   0 0 0 0 .8
                                                   0 0 0 0 .3
                                                   0 0 0 1 0" />
                                    <feComposite operator="out" in="a" in2="SourceGraphic"/>
                                </filter>
                                <filter id="ghost" width="2000000%" height="2000000%" x="-1000000%" y="-1000000%">
                                    <feColorMatrix type="saturate" values=".2"/>
                                </filter>
                                </defs>
                                <!--We nest a g in here because the svg tag itself cannot do transforms
                                    we separate topologies, edges, and nodes to create an explicit z-order
                                -->
                                <g id="transform_ur_viz">
                                <g id="topology_ur_viz"/>
                                <g id="edge1_ur_viz"/>
                                <g id="anchor_ur_viz"/>
                                <g id="node_ur_viz"/>
                                <g id="dialogBox_ur_viz"/>
                                <g id="volumeDialogBox_ur_viz"/>
                                <g id="switchPopup_ur_viz"/>
                                <g id="parentPort_ur_viz"/>
                                <g id="edge2_ur_viz" />
                                <g id="port_ur_viz"/>
                                <g id="volume_ur_viz"/>

                                </g>
                                </svg>                            
                        </td>
                    </tbody>
                </table>            
        </div>
        <!-- TAG PANEL -->       
        <div id="tag-panel"> 
        </div>        
        <!-- JS -->
        <script>
            $(function () {
                $("#sidebar").load("/VersaStack-web/sidebar.html", function () {
                    if (${user.isAllowed(1)}) {
                        var element = document.getElementById("service1");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(2)}) {
                        var element = document.getElementById("service2");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(3)}) {
                        var element = document.getElementById("service3");
                        element.classList.remove("hide");
                    }
                    if (${user.isAllowed(4)}) {
                        var element = document.getElementById("service4");
                        element.classList.remove("hide");
                    }
                });
                $("#tag-panel").load("/VersaStack-web/tagPanel.jsp", null);
            });
        </script>        
    </body>
</html>