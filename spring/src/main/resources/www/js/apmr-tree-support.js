	var metricTree = null;	
	var treeClickChart = null;
	var metricModelCache = {};
	function rebuildNamespace(value) {
		value.shift(); value.shift(); value.shift(); value.shift();
		return value.join('');				
	}
	function splitRootPlus(value) {
		var frags = value.split('/');
		while(frags[0]=="") {
			frags.shift();
		}
		$.each(frags, function(index){
			frags[index] = "/" + frags[index]; 
		});
		return frags;				
	}
	function mapSize(value) {
		var cnt = 0;
		$.each(value, function(key, value){
			cnt++;
		});
		return cnt;				
	}
	
	function reapDownedNodes() {
		var callback = reapDownedNodes;
		if($.apmr.config.unloadDownNodes) {
			try {
				var timeNow = new Date().getTime();
				var cnt = 0;
				//console.info("Downed Node Reaper Running....");
				
				$('[apmr_dnode_downTime]').each(function(index, node){
					var dt = parseInt($(node).attr('apmr_dnode_downTime'));
					if(timeNow-dt>=$.apmr.config.downedNodeLingerTime) {
						console.info("Reaping Node [%o]", node);
						$('#metricTree').jstree("remove", $(node).attr('id'));
						$(node).remove();
						cnt++;
					}
				});
				if(cnt>0) {
					console.info("Reaper Removed [%s] Downed Nodes", cnt);
				}
			} catch (e) {
				console.error("ReapDownedNodes Failed:[%o],%o", e, e.stack);
				
			}
			$.apmr.config.downedNodeReaper = setTimeout(reapDownedNodes, 2000);
		}
	}
	
	function initMetricTree() {
		$.apmr.config.downedNodeReaper = setTimeout(reapDownedNodes, 2000);
		$('#metricTree')
		.jstree({
			core : { 
				animation : 0
			},
			json_data : {
				progressive_render : true,
				progressive_unload: true,
				data : function(node, callback) {
					populateNode(node, callback);
				}
			},					
			plugins : [ "themes", "ui", "types", "json_data", "crrm", "unique"],
			types : {
				'types' : {
		            'root' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/Helios_Symbol_12_18.png'
		                },
		                valid_children : [ 'domain' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }				                
		            },
		            'domain' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/domain_16_16.png'
		                },
		                valid_children : [ 'domain', 'server', 'down-server' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },				            
		            'server' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/server_16_16.png'
		                }, valid_children : [ 'agent', 'online-agent' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },
		            'down-server' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/down_server.png'
		                }, valid_children : [ 'agent', 'online-agent' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },
		            
		            'online-agent' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/online_agentX_16_16.png'
		                }, valid_children : [ 'metric-folder', 'folder', 'metric' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },				            
		            'agent' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/offline_agentX_16_16.png'
		                }, valid_children : [ 'metric-folder', 'folder', 'metric' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },
		            'metric-folder' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/chart-folder_16_16.png'
		                }, valid_children : [ 'metric-folder', 'folder', 'metric' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }
		            },
		            'folder' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/folder_16_16.png'
		                }, valid_children : [ 'metric-folder', 'folder', 'metric' ],
		                select_node : function(me) {
		                	console.info("Selected Node:[%o]", me);
		                }				                
		            },
		            'metric' : {
		            	'use_data' : true,
		                'icon' : {
		                    'image' : 'img/metric_16_16.png'
		                }, valid_children : [],
		                select_node : function(me) {		                	
		                	console.info("Selected Metric:[%s]", $(me).attr('metric'));
		                	//$('#metric-191').parentsUntil('[rel*=agent]')
		                	
		                	var metricId = parseInt($(me).attr('metric'));
		                	var metric = $(me).attr('metricBody');
		                	var hostName = $('#metric-' + metricId).parentsUntil('[hostn]').last().parent().attr('hostn');
		                	var agentName = $('#metric-' + metricId).parentsUntil('[agentn]').last().parent().attr('agentn');
		                	$.apmr.liveData([metricId], function(tsdata){		                		
		                		var model = createChartModels(tsdata)[metricId];
		                		console.info("Live Data Backlog Model Data for [%s]:%o", metricId, model.extractSeries(2)[0]);
		                		$('#chartContainer').empty();
		                		//metricModelCache
		                		treeClickChart = new Highcharts.Chart({
		                	        chart: {
		                	            renderTo: 'chartContainer',
		                	            animation: false
		                	        },
		                	        xAxis: {
		                	            type: 'datetime'
		                	        },
		                	        yAxis: {
		                	            title: ''
		                	        },			
		                	        subtitle : {
		                	        	text : hostName + ':' + agentName
		                	        },
		                	        loading: {
		                	        	showDuration: 0
		                	        },
		                	        title: {
		                	            text: model.metricDef.ns  
		                	        },
		                	        series: [{
		                	        	name: model.metricDef.name,
		                	            data: model.extractedSeries[ChartModel.AVG],
		                	            animation: false
		                	        }]
		                	    });		                		
		                	});
		                }				                
		            }
		        }						
			}
		});
		metricTree = $.jstree._reference('#metricTree');
		//$.apmr.connect(); 
	}
	
	function parentContainsChild(parentId, childId) {
		if(parentId.split('')[0]!='#') parentId = '#' + parentId;
		metricTree._get_children(parentId).each(function(index, node) {
			if($(node).attr('id')==childId) return true;
		});
		return false;
	}
	
	function populateNode(node, callback) {
		//console.info("Populating Node [%o]", node);
		var parentId = $(node).attr('id');
		if(node==-1) {
			callback([{
				attr: {id: "root", rel: "root"},  
				data : {title: "Helios"}
			}]);
			$("#root").removeClass('jstree-leaf').addClass('jstree-closed');
			return;
		} 				
		var rel = $(node).attr('rel');
		var nodeArray = [];
		var timeNow = new Date().getTime();
		switch(rel) {
			case 'root':
				$.apmr.allDomains(function(data) {
					$.each(data.msg, function(index, domain) {
						var uid = "domain-" + domain.replace('.', '_');
						if($('#' + uid).length==0) {
							nodeArray.push({
								attr: {id: uid, rel: "domain", 'domain' : domain},  
								data : {title: domain}									
							});
						}
					});
					callback(nodeArray); fixOpen(nodeArray);
				});
				break;
			case 'domain':
				var domain = $(node).attr('domain');
				//console.info("Populating Hosts in Domain [%s]", domain);
				$.apmr.hostsByDomain(domain, function(data) {
					$.each(data.msg, function(index, host) {
						var hostName = host.name.split('.').pop();
						var uid = "host-" + host.hostId;
						if($('#' + uid).length==0 && !parentContainsChild(parentId, uid)) {  
							var newNode = {
									attr: {id: uid, rel: host.conn==null ? "down-server" : "server", 'host' : host.hostId, 'hostn' : host.name},  
									data : {title: hostName}									
								};
							nodeArray.push(newNode);
							if(host.conn==null) {
								newNode.attr.apmr_dnode_downTime = timeNow;
							}
							callback([newNode]);
						}
					});
					fixOpen(nodeArray);
				});
				break;
			case 'server':
			case 'down-server':
				var host = $(node).attr('host');
				//console.info("Populating Agents in Server [%s]", host);
				$.apmr.agentsByHost(host, function(data) {
					$.each(data.msg, function(index, agent) {
						var uid = "agent-" + agent.agentId;
						if($('#' + uid).length==0 && !parentContainsChild(parentId, uid)) {
							var newNode = {
									attr: {id: uid, rel: agent.conn!=null ? "online-agent" : "agent", 'agent' : agent.agentId, 'agentn' : agent.name, minl : agent.minl},  
									data : {title: agent.name}									
								};
							nodeArray.push(newNode);
							if(agent.conn==null) {
								newNode.attr.apmr_dnode_downTime = timeNow;
							}
							callback([newNode]);
						}
					});
					fixOpen(nodeArray);
				});
				break;
			case 'agent':
			case 'online-agent':
				var agentId = $(node).attr('agent');
				var level = 0;
				var parent = "";
				$.apmr.findLevelMetricsForAgent(0, agentId, function(data) {							
					$.each(data.msg, function(index, metric) {
						var uid = "metric-" + metric.id;
						if($('#' + uid).length==0) {
							var newNode = {
									attr: {id: uid, rel: "metric", 'metric' : metric.id},  
									data : {title: metric.name}									
								};
							callback([newNode]);
							metricModelCache[metric.id] = metric;
							$('#' + uid).livequery(function(){}, function(){
								delete metricModelCache[metric.id];
								console.info("Deleted metricModelCache Entry [%s]", metric.id);
							});							
						}
					});							
					
					
					$.apmr.findLevelFoldersForAgent(level, agentId, parent, function(data) {
						$.each(data.msg, function(index, arr) {
							var folder = arr[0];
							var mlevel = arr[1];
							var isMetricFolder = (mlevel-level==1);														
							var uid = "folder-" + agentId + "-" + folder.replace('=', '_');
							if($('#' + uid).length==0) {
								var newNode = {
										attr: {id: uid, rel: isMetricFolder ? "metric-folder" : "folder", 'folder' : folder, 'agent' : agentId, 'level' : level},  
										data : {title: folder}									
									};
								nodeArray.push(newNode);
								callback([newNode]);
							}
						});							
						fixOpen(nodeArray);
					});						
				});
				break;
			case 'folder':
			case 'metric-folder':
				var agentId = $(node).attr('agent');
				var level = parseInt($(node).attr('level')) +1;
				var parent = $(node).attr('folder');
				//console.info("Populating Level Metrics and Folder [%s]", parent);
				$.apmr.findLevelMetricsForAgentWithParent(level, agentId, parent, function(data) {							
					$.each(data.msg, function(index, metric) {
						var uid = "metric-" + metric.id;
						if($('#' + uid).length==0) {
							var newNode = {
									attr: {id: uid, rel: "metric", 'metric' : metric.id},  
									data : {title: metric.name}									
								}; 
							callback([newNode]);							
							$('#' + $(node).attr('id')).attr('rel', 'metric-folder');
							metricModelCache[metric.id] = metric;
							$('#' + uid).livequery(function(){}, function(){
								delete metricModelCache[metric.id];
								console.info("Deleted metricModelCache Entry [%s]", metric.id);
							});
						}
					});							
					//fixOpen(nodeArray);
					
					$.apmr.findLevelFoldersForAgent(level, agentId, parent, function(data) {
						$.each(data.msg, function(index, arr) {
							var folder = arr[0];
							var mlevel = arr[1];
							var isMetricFolder = (mlevel-level==1);														
							if(folder!=null) {
								var uid = "folder-" + agentId + "-" + folder.replace('=', '_');
								if($('#' + uid).length==0) {
									var newNode = {
											attr: {id: uid, rel: isMetricFolder ? "metric-folder" : "folder", 'folder' : folder, 'agent' : agentId, 'level' : level},  
											data : {title: folder}									
										};
									callback([newNode]);
									nodeArray.push(newNode);
								}
							}
						});							
						fixOpen(nodeArray);
					});						
				});
				break;
		}
		
	}
	function fixOpen(nodeArray) {
		$.each(nodeArray, function(index, node){
			$("#" + node.attr.id).removeClass('jstree-leaf').addClass('jstree-closed');
			
		});
	}
	
	var ChartModel = Class.extend({
		extractedSeries : new Object(),
		metricDef : null,
		metricId : -1,
		rawData : null,
		
		init: function(m, d){
			this.metricDef = m;
			this.rawData = d;
			this.metricId = m.id;
			var st = new Date().getTime();
			for(var i = 0, l = 4; i < l; i++) {
				this.extractSeries(i);
			}			
			var endt = new Date().getTime();
			console.info("EXS:%s ms.", (endt-st));
		},
		extractSeries : function(series) {
			if(series < ChartModel.MIN || series > ChartModel.CNT) throw "Invalid Series Type:" + series;
			if(this.extractedSeries[series]!=null) return this.extractedSeries[series];
			var arr = [];
			$.each(this.rawData, function(ts, values) {
				arr.push([ts, values[0][series]]);
			});
			this.extractedSeries[series] = arr;
			return arr;
		}
	});
	
	ChartModel.MIN = 0;
	ChartModel.MAX = 1;
	ChartModel.AVG = 2;
	ChartModel.CNT = 3;
	

	function createChartModels(json) {
		var chartModels = {};
		$.each(json.msg, function(index, entry){
			if('rerid'!=entry && 't'!=entry ) {
				var id = -1;
				try { id = parseFloat(index) } catch (e) {}
				if(id!=-1) {
					var metric = metricModelCache[id];
					if(metric!=null) {
						chartModels[id] = new ChartModel(metric, json.msg[id]);
						metricModelCache[id] = chartModels[id]; 
					}
				}
			}
		});
		return chartModels;
	}	
			
/*
		$('li.jstree-closed>a').livequery(function(){
			var node = this;
			var id = $(node).parent().attr('id');
			if(!$('#' + id).hasClass('tooltip-set')) {
				$('#' + id).addClass('tooltip-set');
				$('#' + id).qtip({
					   style: { name: 'cream', tip: true },
					   content: id,
					   show: 'mouseover',
					   hide: 'mouseout'
					})
				metricTree.get_path($('#' + id))
				console.info("CREATED NODE:[%o]", $('#metricTree').jstree("_get_node", $(node).parent()));	
			}			
		});
		$('li.jstree-open>a').livequery(function(){
			var node = this;
			var id = $(node).parent().attr('id');
			if(!$('#' + id).hasClass('tooltip-set')) {
				$('#' + id).addClass('tooltip-set');
				$('#' + id).qtip({
					   style: { name: 'cream', tip: true },
					   content: id,
					   show: 'mouseover',
					   hide: 'mouseout'
					})
				metricTree.get_path($('#' + id))				
				console.info("CREATED NODE:[%o]", $('#metricTree').jstree("_get_node", $(node).parent()));	
			}			
		});

*/	