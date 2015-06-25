"use strict";

//For debug purpuses only
var selectedNode;
var debugPoint = {x: 0, y: 0};
define([
    "local/d3", "local/versastack/utils"
], function (d3, utils) {
    var map_ = utils.map_;

    var settings = {
        NODE_SIZE: 30,
        SERVICE_SIZE: 10,
        TOPOLOGY_SIZE: 15,
        TOPOLOGY_BUFFER: 30,
        HULL_COLORS: ["rgb(0,100,255)", "rgb(255,0,255)"],
        HULL_OPACITY: .2,
        EDGE_COLOR: "rgb(0,0,0)",
        EDGE_WIDTH: 2
    };

    var redraw_;

    /**@param {outputApi} outputApi
     * @param {Model} model
     **/
    function doRender(outputApi, model) {
        //outputApi may start zoomed in, as a workaround for the limit of how 
        //far out we can zoom. in order to prevent changes in this parameter 
        //affecting the meaning of our size related parameters, we scale them 
        //appropriatly
        settings.NODE_SIZE /= outputApi.getZoom();
        settings.SERVICE_SIZE /= outputApi.getZoom();
        settings.TOPOLOGY_SIZE /= outputApi.getZoom();
        settings.TOPOLOGY_BUFFER /= outputApi.getZoom();
        settings.EDGE_WIDTH /= outputApi.getZoom();

        var svgContainer = outputApi.getSvgContainer();

        redraw();

        function redraw() {
            svgContainer.select("#topology").selectAll("*").remove();//Clear the previous drawing
            svgContainer.select("#edge").selectAll("*").remove();//Clear the previous drawing
            svgContainer.select("#node").selectAll("*").remove();//Clear the previous drawing
            var nodeList = model.listNodes();
            var edgeList = model.listEdges();

            //Recall that topologies are also considered nodes
            //We render them seperatly to enfore a z-ordering
            map_(nodeList, drawTopology);
            map_(edgeList, drawEdge);
            map_(nodeList, drawNode);

        }
        redraw_ = redraw;
        /**@param {Node} n**/
        function drawNode(n) {
            if (n.isLeaf()) {
                var x = n.x - settings.NODE_SIZE / 2;
                var y = n.y - settings.NODE_SIZE / 2;
                svgContainer.select("#node").append("image")
                        .attr("xlink:href", n.getIconPath())
                        .attr("x", x)
                        .attr("y", y)
                        .attr('height', settings.NODE_SIZE)
                        .attr('width', settings.NODE_SIZE)
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .on("mousemove", onNodeMouseMove.bind(undefined, n))
                        .on("mouseleave", onNodeMouseLeave)
                        .call(makeDragBehaviour(n));
                y += settings.NODE_SIZE;//make the services appear below the node
                map_(n.services, /**@param {Service} service**/function (service) {
                    svgContainer.select("#node").append("image")
                            .attr("xlink:href", service.getIconPath())
                            .attr("x", x)
                            .attr("y", y)
                            .attr('height', settings.SERVICE_SIZE)
                            .attr('width', settings.SERVICE_SIZE)
                            //The click events fold move, and select nodes, in 
                            //which case, we want to behave the same regardless
                            //of if a node or its service was the target. In 
                            //contrast, the mousMove event is for the popup, and
                            //we may want to display different info when we
                            //hover over a service
                            .on("click", onNodeClick.bind(undefined, service))
                            .on("dblclick", onNodeDblClick.bind(undefined, n))
                            .on("mousemove", onNodeMouseMove.bind(undefined, service))
                            .on("mouseleave", onNodeMouseLeave)
                            .call(makeDragBehaviour(n));
                    x += settings.SERVICE_SIZE;
                });
            }

        }
        /**@param {Node} n**/
        function drawTopology(n) {
            if (!n.isLeaf()) {
                //render the convex hull surounding the decendents of n
                var leaves = n.getLeaves();
                var isSingleton = false;
                if (leaves.length === 0) {
                    return;
                }
                if (leaves.length === 1) {
                    //If all leaves are the same point, then the hull will be just
                    //A single point, and not get rendered.
                    //By forcing it to take distinct points, the stroke-width 
                    //Causes it to render at full size
                    var leaf = leaves[0];
                    leaves.push({x: leaf.x + .01, y: leaf.y + .01});
                    isSingleton = true;
                }
                while (leaves.length < 3) {
                    //Even with two distinct point, the path will not exist
                    //Adding a third point (even if it is a duplicate) seems to fix this
                    leaves.push(leaves[0]);
                }

                var path = d3.geom.hull()
                        .x(function (n) {
                            return n.x;
                        })
                        .y(function (n) {
                            return n.y;
                        })
                        (leaves);
                var color = settings.HULL_COLORS[n.getDepth() % settings.HULL_COLORS.length];
                svgContainer.select("#topology").append("path")
                        .style("fill", color)
                        .style("stroke", color)
                        .style("stroke-width", settings.TOPOLOGY_SIZE + settings.TOPOLOGY_BUFFER * n.getHeight())
                        .style("stroke-linejoin", "round")
//                        .style("stroke-opacity", settings.HULL_OPACITY)
//                        .style("fill-opacity", settings.HULL_OPACITY)
                        .style("opacity", settings.HULL_OPACITY)
                        .datum(path)
                        .attr("d", function (d) {
                            //@param d is the datum set above
                            //see https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/d
                            if (d.length === 0) {
                                return;
                            }
                            var ans = "M" + d[0].x + " " + d[0].y + " ";
                            for (var i = 1; i < d.length; i++) {
                                ans += "L" + d[i].x + " " + d[i].y + " ";
                            }
                            ans += "Z";
                            return ans;
                        })
                        .on("click", onNodeClick.bind(undefined, n))
                        .on("dblclick", onNodeDblClick.bind(undefined, n))
                        .on("mousemove", onNodeMouseMove.bind(undefined, n))
                        .on("mouseleave", onNodeMouseLeave)
                        .call(makeDragBehaviour(n));
                var serviceContainer = svgContainer.select("#node").append("g");
                if (!isSingleton) {
                    //Get the highest point.
                    var ans=0;
                    for(var i=0; i<path.length; i++){
                        if(path[i].y<path[ans].y){
                            ans=i;
                        }
                    }
                    var p1=path[ans];
                    //we know want to determine which neighbors of path[ans] give the shallower edge
                    var left=ans===0?path.length-1:ans-1;
                    var right=(ans+1)%path.length;
                    left=path[left];
                    right=path[right];
                    var leftSlope=Math.abs((p1.y-left.y)/(p1.x-left.x));
                    var rightSlope=Math.abs((p1.y-right.y)/(p1.x-right.x));
                    var p2;
                    if(leftSlope<rightSlope){
                        p2=left;
                    }else{
                        p2=right;
                    }
                    
                    var p = {x: (p1.x + p2.x) / 2, y: (p1.y + p2.y) / 2};
                    //compute the desired distance between the services, and the line p1p2 
                    var normalOffset = settings.TOPOLOGY_SIZE / 2 + settings.TOPOLOGY_BUFFER / 2 * (n.getHeight()) + settings.SERVICE_SIZE;
                    //convert the above offset into the xy plane, and apply it to p
                    var theta = Math.atan2(p1.y - p2.y, p1.x - p2.x);
                    if(theta<-Math.PI/2){
                        theta+=Math.PI;
                    }
                    if(theta>Math.PI/2){
                        theta-=Math.PI;
                    }
                    p.x+=normalOffset*Math.sin(theta);
                    p.y-=normalOffset*Math.cos(theta);
                    
                    var x = p.x - settings.SERVICE_SIZE * n.services.length / 2;
                    var y = p.y;
                    serviceContainer.attr("transform", "rotate(" + theta*180/Math.PI + " " + p.x + " " + (y + settings.SERVICE_SIZE / 2) + ")");
                } else {
                    x = path[0].x - settings.SERVICE_SIZE * n.services.length / 2;
                    y = path[0].y - settings.TOPOLOGY_SIZE / 2 - settings.TOPOLOGY_BUFFER / 2 * (n.getHeight()) - settings.SERVICE_SIZE;
                }
                map_(n.services, /**@param {Service} service**/function (service) {
                    serviceContainer.append("image")
                            .attr("xlink:href", service.getIconPath())
                            .attr("x", x)
                            .attr("y", y)
                            .attr('height', settings.SERVICE_SIZE)
                            .attr('width', settings.SERVICE_SIZE)
                            //The click events fold move, and select nodes, in 
                            //which case, we want to behave the same regardless
                            //of if a node or its service was the target. In 
                            //contrast, the mousMove event is for the popup, and
                            //we may want to display different info when we
                            //hover over a service
                            .on("click", onNodeClick.bind(undefined, service))
                            .on("dblclick", onNodeDblClick.bind(undefined, n))
                            .on("mousemove", onNodeMouseMove.bind(undefined, service))
                            .on("mouseleave", onNodeMouseLeave)
                            .call(makeDragBehaviour(n));
                    x += settings.SERVICE_SIZE;
                });


//                //Debug, show the coordinate of the topology node itself
//                svgContainer.select("#topology").append("circle")
//                        .attr("cx", n.x)
//                        .attr("cy", n.y)
//                        .attr("r", 2)
//                        .style("fill", "red")
            }

        }

        /**@param {Node} n**/
        
        var lastMouse;
        function makeDragBehaviour(n) {
            return d3.behavior.drag()
                    .on("drag", function () {
                        //Using the dx,dy from d3 can lead to some artifacts when also using
                        //These seem to occur when moving between different transforms
                        var e = d3.event.sourceEvent;
                        var dx=(e.clientX-lastMouse.clientX)/outputApi.getZoom();
                        var dy=(e.clientY-lastMouse.clientY)/outputApi.getZoom();
                        lastMouse=e;
                        move(n, dx, dy);
                        redraw();
                    })
                    .on("dragstart", function () {
                        lastMouse=d3.event.sourceEvent;
                        outputApi.disablePanning();
                    })
                    .on("dragend", function () {
                        outputApi.enablePanning();
                    });
        }

        /**@param {Edge} e**/
        function drawEdge(e) {
            var src = e.source.getCenterOfMass();
            var tgt = e.target.getCenterOfMass();
            svgContainer.select("#edge").append("line")
                    .attr("x1", src.x)
                    .attr("y1", src.y)
                    .attr("x2", tgt.x)
                    .attr("y2", tgt.y)
                    .style("stroke", settings.EDGE_COLOR)
                    .style("stroke-width", settings.EDGE_WIDTH);
        }

        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeClick(n) {
            d3.event.stopPropagation();//prevent the click from being handled by the background, which would hide the panel
            outputApi.setActiveName(n.getName());
            var services = [];
            if (n.services) {
                services = map_(n.services, /**@param {Service} service**/function (service) {
                    return service.getTypeBrief();
                });
            }
            outputApi.setServices(services);
            selectedNode = n;
        }
        /**
         * Note that n could also be a topology
         * @param {Node} n**/
        function onNodeDblClick(n) {
            //We will never send a mouseleave event as the node is being removed
            document.getElementById("hoverdiv").style.visibility = "hidden";
            //The coordinates provided seem not to line up with where the mouse is,
            //So we use the center of mass to stay consistent
            var e = d3.event;
            var chords = n.getCenterOfMass();
            n.toggleFold();
            if (n.isFolded) {
                //there is no guarantee that n is posistioned anywhere near its children
                //to solve this, we force n to be located at the click
                n.x = chords.x;
                n.y = chords.y;
            }
            redraw();
        }
        function onNodeMouseMove(n) {
            var hovertext = n.getName();
            document.getElementById("hoverdiv").innerText = hovertext;
            document.getElementById("hoverdiv").style.left = d3.event.x + "px";
            document.getElementById("hoverdiv").style.top = d3.event.y + 10 + "px";
            document.getElementById("hoverdiv").style.visibility = "visible";
        }

        function onNodeMouseLeave() {
            document.getElementById("hoverdiv").style.visibility = "hidden";
        }

        /**@param {Node} n**/
        function move(n, dx, dy) {
            n.x += dx;
            n.y += dy;
            map_(n.children, function (child) {
                move(child, dx, dy);
            });
        }
    }


    return{
        doRender: doRender,
        redraw: function () {
            redraw_();
        }
    };
});