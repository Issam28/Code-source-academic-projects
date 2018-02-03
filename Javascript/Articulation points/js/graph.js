// set up SVG for D3
// size affects iframe resizing in the main app page



var script = document.createElement('script');
script.src = 'js/jquery.js';
script.type = 'text/javascript';
document.getElementsByTagName('head')[0].appendChild(script);

var width = 1300,
    height = 500,
    linkDistance = 100,
    xbound, // these bounds will be dynamically set by main page
    ybound,
    colors = d3.scale.category10(),
    undoStack = [],
    redoStack = [],
    maxStackSize = 20,
    curAlist;   // current adjacency list (directed)

var svg = d3.select('body')
    .append('svg')
    .attr('oncontextmenu', 'return false;')
    .attr('width', width)
    .attr('height', height)
    .attr('id', 'mainSvg');


// set up initial nodes and links
//  - nodes are known by 'id', not by index in array.
//  - reflexive edges are indicated on the node (as a bold black circle).
//  - links are always source < target; edge directions are set by 'left' and 'right'.
//  - $visited helps with graph traversal
// specify initial positions of nodes to avoid random (by default) behavior at beginning
var nodes = [],
  lastNodeId = -1,
  links = [];

// init D3 force layout
var force = d3.layout.force()
    .nodes(nodes)
    .links(links)
    .size([width, height])
    .linkDistance(linkDistance)
    .charge(-400)
    .on('tick', tick)

// define arrow markers for graph links
svg.append('svg:defs').append('svg:marker')
    .attr('id', '')
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 6)
    .attr('markerWidth', 3)
    .attr('markerHeight', 3)
    .attr('orient', 'auto')
  .append('svg:path')
    .attr('d', 'M100,-5L10,0L0,5')
    .attr('fill', '#000');

svg.append('svg:defs').append('svg:marker')
    .attr('id', '')
    .attr('viewBox', '0 -5 10 10')
    .attr('refX', 4)
    .attr('markerWidth', 3)
    .attr('markerHeight', 3)
    .attr('orient', 'auto')
  .append('svg:path')
    .attr('d', 'M10,-5L0,0L10,5')
    .attr('fill', '#000');

// line displayed when dragging new nodes
var drag_line = svg.append('svg:path')
  .attr('class', 'link dragline hidden')
  .attr('d', 'M0,0L0,0');

// handles to link and node element groups
var path = svg.append('svg:g').selectAll('path'),
    circle = svg.append('svg:g').selectAll('g');

// mouse event vars
var selected_node = null,
    selected_link = null,
    mousedown_link = null,
    mousedown_node = null,
    mouseup_node = null;
    drag_node = null;

function resetMouseVars() {
    mousedown_node = null;
    mouseup_node = null;
    mousedown_link = null;
}

// update force layout (called automatically each iteration)
function tick() {
    // draw directed edges with proper padding from node centers
    path.attr('d', function (d) {
        var deltaX = d.target.x - d.source.x,
            deltaY = d.target.y - d.source.y,
            dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY),
            normX = deltaX / dist,
            normY = deltaY / dist,
            sourcePadding = d.left ? 10 : 12,
            targetPadding = d.right ? 10 : 12,
            sourceX = d.source.x + (sourcePadding * normX),
            sourceY = d.source.y + (sourcePadding * normY),
            targetX = d.target.x - (targetPadding * normX),
            targetY = d.target.y - (targetPadding * normY);
        return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY;
    });

    // move all the circles to current positions by updating the transform attributes in svg container
    circle.attr('transform', function (d) {
        // update position against border
        if (xbound && ybound) {
            var prevdx = d.x, prevdy = d.y;
            d.x = Math.max(15, Math.min(xbound - 25, d.x));
            d.y = Math.max(15, Math.min(ybound - 25, d.y));
            if (prevdx != d.x || prevdy != d.y) {
                d3.select(this).classed("fixed", d.fixed = false); // set fixed bit to false
                if (d === drag_node) { // if the node is being dragged outside border, release mouse early to prevent crazy rebounce
                    var event = document.createEvent("Event");  // "SVGEvents" would've been the more appropriate event type but IE doesn't like it
                    event.initEvent("mouseup", true, true);
                    this.dispatchEvent(event);
                }
            }
        }

        return 'translate(' + d.x + ',' + d.y + ')';
    });
}

// update graph (called when needed)
function restart() {
    // bind links to path group
    path = path.data(links);

    // update existing links based on their states (dynamically set classes for CSS animation: http://jaketrent.com/post/d3-class-operations/)
    path.classed('selected', function (d) { return d === selected_link; })
      .style('marker-start', function (d) { return d.left ? 'url(#start-arrow)' : ''; })
      .style('marker-end', function (d) { return d.right ? 'url(#end-arrow)' : ''; });

    // add new links
    path.enter().append('svg:path')
      .attr('class', 'link')
      .classed('selected', function (d) { return d === selected_link; })
      .style('marker-start', function (d) { return d.left ? 'url(#start-arrow)' : ''; })
      .style('marker-end', function (d) { return d.right ? 'url(#end-arrow)' : ''; })
      .on('mousedown', function (d) {
          if (d3.event.ctrlKey) return;

          // select link
          mousedown_link = d;
          if (mousedown_link === selected_link) selected_link = null;
          else selected_link = mousedown_link;
          selected_node = null;
          restart();
      });

    // remove old links
    path.exit().remove();

    // circle (node) group
    // NB: the function arg is crucial here! nodes are known by id, not by index!
    circle = circle.data(nodes, function (d) { return d.id; });

    // update existing nodes (reflexive & selected visual states)
    circle.selectAll('circle')
      .style('fill', function (d) { return (d === selected_node) ? d3.rgb(colors(d.id)).brighter().toString() : colors(d.id); })
      .classed('reflexive', function (d) { return d.reflexive; });

    // add new nodes
    var g = circle.enter().append('svg:g');



    // show node IDs
    g.append('svg:text')
        .attr('id',function (d) { return 'MA'+d.id; });


    g.append('svg:circle')
      .attr('class', 'node')
      .attr('r', 18)

      .style('fill', function (d) { return (d === selected_node) ? d3.rgb(colors(d.id)).brighter().toString() : colors(d.id); })
      .style('stroke', function (d) { return d3.rgb(colors(d.id)).darker().toString(); })
      .classed('reflexive', function (d) { return d.reflexive; })
      .on('mouseover', function (d) {
          if (!mousedown_node || d === mousedown_node) return;
          // enlarge target node
          d3.select(this).attr('transform', 'scale(1.1)');
      })
      .on('mouseout', function (d) {
          if (!mousedown_node || d === mousedown_node) return;
          // unenlarge target node
          d3.select(this).attr('transform', '');
      })
      .on('mousedown', function (d) {
          if (d3.event.ctrlKey) { drag_node = d; return; }

          // select node
          mousedown_node = d;
          if (mousedown_node === selected_node) selected_node = null;
          else selected_node = mousedown_node;
          selected_link = null;

          // reposition drag line
          drag_line
            .style('marker-end', 'url(#end-arrow)')
            .classed('hidden', false)
            .attr('d', 'M' + mousedown_node.x + ',' + mousedown_node.y + 'L' + mousedown_node.x + ',' + mousedown_node.y);

          restart();
      })
      .on('mouseup', function (d) {
          if (!mousedown_node) return;

          // needed by FF
          drag_line
            .classed('hidden', true)
            .style('marker-end', '');

          // check for drag-to-self
          mouseup_node = d;
          if (mouseup_node === mousedown_node) { resetMouseVars(); return; }

          // unenlarge target node
          d3.select(this).attr('transform', '');

          // add link to graph (update if exists)
          // NB: links are strictly source.id < target.id; arrows separately specified by booleans
          var source, target, direction;
          // 'right' is the conventional direction of source to target; 'left' is the reverse
          if (mousedown_node.id < mouseup_node.id) {
              source = mousedown_node;
              target = mouseup_node;
              direction = 'right';
          } else {
              source = mouseup_node;
              target = mousedown_node;
              direction = 'left';
          }

          // check and see if a link with specified source or target exists
          var link;
          link = links.filter(function (l) {
              return (l.source === source && l.target === target);
          })[0];

          // if a link already exists, update its left/right values with new direction; else create new link
          if (link) {
              link[direction] = true;
          } else {
              link = { source: source, target: target, left: false, right: false };
              link[direction] = true;
              links.push(link);
          }

          // select new link after creation for convenient editing
          selected_link = link;
          selected_node = null;
          AP();
          updateAppStatus();
          restart();
      })
    .on("dblclick", dblclick);


    // show node IDs
    g.append('svg:text')
        .attr('x', 0)
        .attr('y', 4)

        .attr('class', 'id')
        .text(function (d) { return d.id; });

    // remove old nodes
    circle.exit().remove();

    // set the graph in motion
    force.start();
}

function mousedown() {
    // prevent I-bar on drag
    //d3.event.preventDefault();

    // because :active only works in WebKit?
    svg.classed('active', true);

    if (d3.event.ctrlKey || mousedown_node || mousedown_link) {return;}

    // insert new node at point
    var point = d3.mouse(this),
        node = { id: ++lastNodeId, reflexive: false, visited: false };
    node.x = point[0];
    node.y = point[1];
    nodes.push(node);
    updateAppStatus();
    restart();
}

function mousemove() {
    if (!mousedown_node) return;

    // update drag line
    drag_line.attr('d', 'M' + mousedown_node.x + ',' + mousedown_node.y + 'L' + d3.mouse(this)[0] + ',' + d3.mouse(this)[1]);

    restart();
}

function mouseup() {
    if (mousedown_node) {
        // hide drag line
        drag_line
          .classed('hidden', true)
          .style('marker-end', '');
    }

    // because :active only works in WebKit?
    svg.classed('active', false);

    // clear mouse event vars
    resetMouseVars();
}

// helper function to clean up links on node removal
// remove all the links that originate or end at the node
function spliceLinksForNode(node) {
    var toSplice = links.filter(function (l) {
        return (l.source === node || l.target === node);
    });
    toSplice.map(function (l) {
        links.splice(links.indexOf(l), 1);
    });
}

// only respond once per keydown
var lastKeyDown = -1;

function keydown() {
    d3.event.preventDefault();

    if (lastKeyDown !== -1) return;
    lastKeyDown = d3.event.keyCode;

    // ctrl, allow dragging
    if (d3.event.keyCode === 17) {
        circle.call(drag);
        svg.classed('ctrl', true);
    }

    if (!selected_node && !selected_link) return;

    switch (d3.event.keyCode) {
        case 8: // backspace
        case 46: // delete
            if (selected_node) {
                nodes.splice(nodes.indexOf(selected_node), 1);
                spliceLinksForNode(selected_node);
            } else if (selected_link) {
                links.splice(links.indexOf(selected_link), 1);
            }
            selected_link = null;
            selected_node = null;
            AP ();
            updateAppStatus();

            restart();
            break;
        case 66: // B
            if (selected_link) {
                // set link direction to both left and right
                selected_link.left = true;
                selected_link.right = true;
            }
           updateAppStatus();
            restart();
            break;
        case 76: // L
            if (selected_link) {
                // set link direction to left only
                selected_link.left = true;
                selected_link.right = false;
            }
            updateAppStatus();
            restart();
            break;
        case 82: // R
            if (selected_node) {
                // toggle node reflexivity
                selected_node.reflexive = !selected_node.reflexive;
            } else if (selected_link) {
                // set link direction to right only
                selected_link.left = false;
                selected_link.right = true;
            }
            updateAppStatus();
            restart();
            break;
    }
}

function keyup() {
    lastKeyDown = -1;

    // ctrl
    if (d3.event.keyCode === 17) {
        circle
          .on('mousedown.drag', null)
          .on('touchstart.drag', null);
        svg.classed('ctrl', false);
    }
}

// specify fixed bit for sticky dragging effect
var drag = force.drag()
    .on("dragstart", dragstart);
function dblclick(d) {
    d3.select(this).classed("fixed", d.fixed = false);
}
function dragstart(d) {
    d3.select(this).classed("fixed", d.fixed = true);
}

// return the adjacency list describing current graph relation, in the form of node0id:[node1ref, node2ref...]
// the $undirected switch is optional; if provided (conventionally set to 1), produce the adjacency list of 
// underlying undirected graph instead of default directed graph
function getAdjlist(undirected) {
    var alist = {};

    // add all nodes/links
    for (var i = 0; i < links.length; i++) {
        var lnk = links[i];
        var src = lnk.source, tgt = lnk.target;
        var srcId = lnk.source.id, tgtId = lnk.target.id;
        if (!alist.hasOwnProperty(srcId))
            alist[srcId] = [];
        if (!alist.hasOwnProperty(tgtId))
            alist[tgtId] = [];
        if (typeof undirected != "undefined") {
            alist[srcId].push(tgt);
            alist[tgtId].push(src);
        } else {
            if (alist[srcId].indexOf(tgt) == -1 && lnk.right) // if src doesn't already have tgt in its list and lnk points from src to tgt
                alist[srcId].push(tgt);
            if (alist[tgtId].indexOf(src) == -1 && lnk.left) // if lnk points from target to source (both ways allowed)
                alist[tgtId].push(src);
        }
    }

    // add all loops
    for (var j = 0; j < nodes.length; j++) {
        var n = nodes[j];
        var nid = nodes[j].id;
        if (!alist.hasOwnProperty(nid))
            alist[nid] = [];
        if (n.reflexive)
            alist[nid].push(n);
    }

    // sort the individual arrays
    for (var nd in alist) {
        if (alist.hasOwnProperty(nd))
            alist[nd].sort(function (a, b) { return a.id - b.id });
    }

    // convert the node references in arrays to conventional id representation
    alist.toNumList = function () {
        var numList = {}; // create a copy; no modification on original
        for (var nd in this)
            if (nd !== "toNumList" && this.hasOwnProperty(nd)) {
                numList[nd] = [];
                for (var x = 0; x < this[nd].length; x++)
                    numList[nd][x] = this[nd][x].id;
            }
        return numList;
    };
    return alist;
}



// creates/updates the graph from a given (valid) 'list' object (e.g. {"0':[],'1':['0','1']})
// called when the adjacency list changes
function createFromList(list) {
    // create shallow copy containers to hold nodes/links that need to be removed
    var nodesToGo = nodes.slice();
    var linksToGo = links.slice();
    var loopNodesToGo = []; // treat loops (self-directed links) separately

    for (var n = 0; n < nodes.length; n++)
        if (nodes[n].reflexive)
            loopNodesToGo.push(nodes[n])

    // add nodes if not present
    for (var src in list) {
        var newNode = true,
            srcId = parseInt(src, 10);
        for (var n = 0; n < nodesToGo.length; n++) {
            if ((srcId == nodesToGo[n].id)) {
                newNode = false;
                nodesToGo.splice(nodesToGo.indexOf(nodesToGo[n]), 1);
                break;
            }
        }
        if (newNode)
            nodes.push({ id: srcId, reflexive: false, visited: false });
    }

    // remove leftover nodes and their associated links
    nodesToGo.map(function (n) {
        nodes.splice(nodes.indexOf(n), 1);
        spliceLinksForNode(n);
    });

    // remove link directions; add them in when present
    for (var l = 0; l < links.length; l++)
        links[l].left = links[l].right = false;

    // now deal with edges/links and loops
    for (var src in list) {
        if (src > lastNodeId) lastNodeId = src; // update lastNodeId to the largest in list
        var srcId = parseInt(src, 10);
        var srcNode = nodes.filter(function (n) { return n.id == srcId })[0];
        for (var i = 0; i < list[src].length; i++) {
            var tgtId = parseInt(list[src][i], 10);
            // if it's a loop
            if (srcId == tgtId) {
                if (srcNode.reflexive) // if this loop exists
                    loopNodesToGo.splice(loopNodesToGo.indexOf(srcNode), 1);
                else
                    srcNode.reflexive = true;
                continue;
            }
            // non-loops
            var direction, source, target, tgtNode = nodes.filter(function (n) { return n.id == tgtId })[0];
            if (srcId < tgtId) {
                source = srcNode;
                target = tgtNode;
                direction = 'right';
            } else {
                source = tgtNode;
                target = srcNode;
                direction = 'left';
            }
            var link = links.filter(function (l) {
                return (l.source === source && l.target === target);
            })[0];
            // if there's a link between the two
            if (link) {
                var idxTg = linksToGo.indexOf(link);
                if (idxTg != -1)
                    linksToGo.splice(idxTg, 1); // preserve this edge
            } else {
                link = { source: source, target: target, left: false, right: false };
                links.push(link);
            }
            link[direction] = true; // enable desired direction
        }
    }

    // remove leftover links not present in the new graph
    linksToGo.map(function (l) {
        var idx = links.indexOf(l);
        if (idx != -1) // only remove those that are actually in links
            links.splice(idx, 1);
    });

    // disable the loops that need removed
    loopNodesToGo.map(function (n) {
        n.reflexive = false;
    });

    // essentially do the same things here as updateAppStatus(), except update adjlistFrame using given input
    // rather than current adjacency list
    if (!(undoing || redoing)) record(curAlist.toNumList());    // DO NOT capture state when called from undo/redo
    curAlist = getAdjlist();


    restart();
}



function clear() {
    createFromList('');
    lastNodeId = -1;
}

var undoBtn = window.parent.document.getElementById('undo'), redoBtn = window.parent.document.getElementById('redo');
var undoing = false, redoing = false;
function updateBtns() {
    if (undoStack.length) undoBtn.className = "";
    else undoBtn.className = "clicked";
    if (redoStack.length) redoBtn.className = "";
    else redoBtn.className = "clicked";

}

// before a change occurs, record the current state, store it onto undo stack
function record(state) {
    redoStack.length = 0;   // empty redo stack
    if (undoStack.length > maxStackSize) undoStack.shift();
    undoStack.push(state);
    updateBtns();
}

function undo() {
    undoing = true;


    if (undoStack.length > 0) {
        var prev = undoStack.pop();
        redoStack.push(curAlist.toNumList());
        createFromList(prev);
    }
    updateBtns();
    undoing = false;
    AP () ;

}

function redo() {
    redoing = true;
    if (redoStack.length > 0) {
        var next = redoStack.pop();
        undoStack.push(curAlist.toNumList());
        createFromList(next);
    }
    updateBtns();
    redoing = false;
    AP () ;

}

// transversal DFS
function Graph() {

    var time = 0;
    var NIL =-1 ;

    this.APUtil = function (  u,   visited,   disc, low,   parent,   ap , adj)
    {
        var children = 0;
        visited[u] = true;
        disc[u] = low[u] = ++time;

        // Go through all vertices adjacent to this
        var j;
        for (j=0; j < adj[u].length ; j++)
        {
            if (!visited[adj[u][j]])
            {
                children++;
                parent[adj[u][j]] = u;
                this.APUtil(adj[u][j], visited, disc, low, parent, ap,adj);

                low[u]  = Math.min(low[u], low[adj[u][j]]);

                if (parent[u] == NIL && children > 1)
                    ap[u] = true;

                if (parent[u] != NIL && low[adj[u][j]] >= disc[u])
                    ap[u] = true;
            }
            else if (adj[u][j] != parent[u])
                low[u]  = Math.min(low[u], disc[adj[u][j]]);
        }

    }

}

// updates



function AP () {

   var curAlist = getAdjlist(1);

    var length =Object.keys(curAlist.toNumList()).length;

    var visited= {};
    var disc = {};
    var low ={};
    var parent = {};
    var ap  = {};  // To store articulation points


    // Initialize parent and visited, and ap(articulation point)
    // arrays

    for (var i = 0; i < length; i++)
    {
        parent[Object.keys(curAlist.toNumList())[i]] = -1;
        visited[Object.keys(curAlist.toNumList())[i]] = false;
        ap[Object.keys(curAlist.toNumList())[i]]= false;
    }

    var A = new Graph ();

    for (i = 0; i < length; i++)
    {
        if (!visited[Object.keys(curAlist.toNumList())[i]])
        A.APUtil(Object.keys(curAlist.toNumList())[i], visited,   disc, low,   parent,   ap , curAlist.toNumList());

    }

    var idd;
    for (i = 0; i < length; i++)
    {
        idd = Object.keys(curAlist.toNumList())[i];
        if (ap[Object.keys(curAlist.toNumList())[i]])
        $("#MA"+idd+"+ circle").css({'stroke': 'rgb(255, 0, 0)'});
        else $("#MA"+idd+"+ circle").css({'stroke': 'rgb(139, 139, 139)'});

    }

}


function updateAppStatus() {

    record(curAlist.toNumList());
    curAlist = getAdjlist();

}

// app starts here
curAlist = getAdjlist();
svg.on('mousedown', mousedown)
  .on('mousemove', mousemove)
  .on('mouseup', mouseup);
d3.select(window)
  .on('keydown', keydown)
  .on('keyup', keyup);
restart();







