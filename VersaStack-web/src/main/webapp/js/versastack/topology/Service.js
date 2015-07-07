"use strict";
define(["local/versastack/topology/modelConstants"], function (values) {
    function Service(backing,owningNode) {
        this._backing = backing;
        this.owningNode=owningNode;
        this.type = "";
        this.svgNode=null;
        this.x=0;
        this.y=0;
        this.dy=0;
        this.dx=0;
        this.size=0;

        this.getTypeBrief = function () {
            return this.type.split("#")[1];
        };
        //Initialization
        //get the type
        var types = this._backing[values.type];

        var that = this;
        map_(types, function (type) {
            type = type.value;
            if (type === values.namedIndividual) {
                return;
            }
            that.type = type;
        });

        this.getIconPath = function () {
            var prefix="/VersaStack-web/resources/";
            var types = this._backing[values.type];
            var ans = iconMap.default;
            map_(types, function (type) {
                type = type.value;
                if (type in iconMap) {
                    ans = iconMap[type];
                } else if (type !== values.namedIndividual) {
                    console.log("No icon registered for type: " + type);
                }
            });
            return prefix+ans;
        };
        
        this.getName = function(){
            return this.type;
        };
        
        var iconMap = {};{
            //The curly brackets are for cold folding purposes
            iconMap["default"] = "default.png";
            iconMap[values.hypervisorService]="hypervisor_service.png";
            iconMap[values.routingService]="routing_service.png";
            iconMap[values.storageService]="storage_service.jpg";
            iconMap[values.objectStorageService]=iconMap[values.storageService];
            iconMap[values.blockStorageService]="block_storage_service.png";
            iconMap[values.IOPerformanceMeasurementService]="io_perf_service.png";
            iconMap[values.hypervisorBypassInterfaceService]="hypervisor_bypass_interface_service.png";
            iconMap[values.virtualSwitchingService]="virtual_switch_service.png";
            iconMap[values.switchingService]="switching_service.png";
            iconMap[values.topopolgySwitchingService]=iconMap[values.switchingService];
//            iconMap[values.virtualCloudService]="";
            
        }
    
        this.getType=function(){
            return "Service";
        };
        
        this.populateTreeMenu=function(tree){
            tree.addChild(this.getTypeBrief());
        };
    }
    return Service;
});