/*global $ d3 */

$(document).ready(function() {
  var svg = d3.select("svg");
  svg.on("click", function() {
    console.log(d3.mouse(this));
    var coordinates = d3.mouse(this);
    setLocation(coordinates[0], coordinates[1], 100);
  });

  svg.attr('width', '90vw');
  svg.attr('height', '90vh');
  //svg.style('margin', '0 60px')
});

function setLocation(x, y, radius) {
    d3.select('svg').append("circle")
                    .attr("cx", x)
                    .attr("cy", y)
                    .attr("r", 15);

    d3.select('svg').append("circle")
                    .attr("cx", x)
                    .attr("cy", y)
                    .style("fill", "#5e8fd1")
                    .style("fill-opacity", "0.59")
                    .attr("r", 10)
                    .transition()
                    .duration(750)
                    .attr("r", radius);
}

function removeAllCircles() {
  d3.selectAll('circle').remove();
}

function changeSVG() {
  d3.xml('svg/SB-02.svg', function (xml) {
    $('body').empty();
    $('body').append(xml.documentElement);
  });
}

function getAspectRatio() {
  const origin = d3.select('.origin').filter('path').node().getBBox();
  const originTop = d3.select('.originTop').filter('path').node().getBBox();
  return ((origin.y + origin.height) - originTop.y)/((originTop.x + originTop.width)- origin.x)
}

function getRealAspectRatio() {
  return parseFloat(d3.select('svg').attr('data-height'), 10)/parseFloat(d3.select('svg').attr('data-width'), 10)
}

function mapX (x) {
  const origin = d3.select('.origin').filter('path').node().getBBox();
  const originTop = d3.select('.originTop').filter('path').node().getBBox();
  const in_min = 0;
  const in_max = parseFloat(d3.select('svg').attr('data-width'), 10);
  const out_min = origin.x;
  const out_max = originTop.x + originTop.width;
  return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}

function mapY (y) {
  const origin = d3.select('.origin').filter('path').node().getBBox();
  const originTop = d3.select('.originTop').filter('path').node().getBBox();
  const in_min = 0;
  const in_max = parseFloat(d3.select('svg').attr('data-height'), 10);
  const out_min = origin.y + origin.height;
  const out_max = originTop.y;
  return (y - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}
function map (num, in_min, in_max, out_min, out_max) {
  return (num - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
}

function setBeacon(x,y) {
  setLocation(mapX(x), mapY(y), 100);
}
