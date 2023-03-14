
var items = [];
var a = arguments[0];
while (a != document) {
  var child = a;
  var i=0; while(child=child.previousElementSibling) i++;
  var node = {tag:null,id:null,index:null,classes:[],other:{},innerText:""};
  if (a.tagName !== undefined) {
    node.tag = a.tagName.toLowerCase();
  }
  node.id = a.id;
  node.index = i;
  node.innerText = a.innerText;

  if (a.hasAttribute("class")) {
	  node.classes = a.attributes.class.value.split(' ');
  }
  for (index = 0; index < a.attributes.length; ++index) {
      var attrName = a.attributes[index].name;
      if (["id","class"].indexOf(attrName) == -1){
		    node.other[attrName] = a.attributes[index].value;
      }
  };

  items.unshift(node);
  a = a.parentNode;
}
return JSON.stringify(items);