/*
 * Healenium-web Copyright (C) 2019 EPAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var items = [];
var a = arguments[0];
while (a != document) {
  var child = a;
  var i=0; while(child=child.previousElementSibling) i++;
  var node = {tag:null,id:null,index:null,classes:[],other:{},innerText:""};
  node.tag = a.tagName.toLowerCase();
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