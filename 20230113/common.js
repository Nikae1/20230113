/**
 * Smart POS - Common Function
 */
/* keyEvent 새로고침 
document.onkeydown = function(event){
	const key = event.keyCode;
	if(key == 116 || (event.ctrlKey && key == 82) || (event.altKey && key == 37)){
		event.preventDefault();
	}
};
*/
 /* PUBLIC IP 수집 */
let publicIp;
function getPublicIp(){
	/* ajax:: get >>  https://api.ipify.org/format=json */ 
	return "106.243.194.229";
}

/* Page Initialize */
function pageInit(messageString, accessInfo){
	publicIp = getPublicIp();
	
	if(messageString != '') messageController(true, message); 
	if(accessInfo != '') document.getElementById("accessInfo").innerText =	"로그아웃(Access Time : " + accessInfo.substr(8,2) + ":" + accessInfo.substr(10, 2) + ":" + accessInfo.substr(12, 2) + ")";
	if(jsonString != '') mgrInit();
}

/* 메세지박스 제어 */
function messageController(turn, messageString){
	let message;
	
	const background = document.getElementById("background");
	const title = document.getElementById("messageTitle");
	const content = document.getElementById("messageContent");
	
	if(turn){
		message = messageString.split(":"); 
		title.innerText = message[0];
		content.innerText = message[1];
		background.style.display = "block";
	}else{
		title.innerText = "";
		content.innerText = "";
		background.style.display = "none";
		if(messageString != '')	{
			message = messageString.split(":");
			window[message[0]](message[1], message[2], message[3]);
		}
	}
}

 /* 문자열이 JSON 데이터 타입인지 여부 */
function isJsonString(str){
	let result;
	try {
    result = (typeof JSON.parse(str) === 'object');
  } catch (e) {
    result = false;
  }
  return result;
}

 /* 서버로 전송할 데이터 길이의 유효성 판단 */
 function lengthCheck(obj){
	
	const dataLength =[["storeCode",10,10], ["storeName",2,50], ["storePhone",10,11], ["empCode", 3, 3], ["empPin", 6, 6], ["groupName",2,20], ["groupCeo",2,5],["groupPin",6,6],["levName", 2, 20]];
	let result = false;
	
	for(let recordIdx=0; recordIdx<dataLength.length; recordIdx++){
		if(obj.getAttribute("name") == dataLength[recordIdx][0]){
			if(dataLength[recordIdx][0] == 'storeCode' || dataLength[recordIdx][0] == 'storePhone'){
				if(obj.value.length >= dataLength[recordIdx][1] 
				&& obj.value.length <= dataLength[recordIdx][2]){
					if(!isNaN(obj.value)) result = true;
				}			
			}else if(obj.value.length >= dataLength[recordIdx][1] 
				&& obj.value.length <= dataLength[recordIdx][2]
				&& isNaN(obj.value.substr(0,1))) result = true;
		} 	
	}

	return result;
}

/* Password Validation */
function isPasswordCheck(text){
	
	const largeChar = /[A-Z]/;
	const smallChar = /[a-z]/;
	const num = /[0-9]/;
	const specialChar = /[!@#$%^&*]/;
	
	let typeCount = 0;
	
	if(largeChar.test(text)) typeCount++;
	if(smallChar.test(text)) typeCount++;
	if(num.test(text)) typeCount++;
	if(specialChar.test(text)) typeCount++; 
		
	return typeCount >= 3? true:false;
}

/* FORM 생성 */
function createForm(name, action, method){
	const form = document.createElement("form");
	if(name != "") form.setAttribute("name", name);
	form.setAttribute("action", action);
	form.setAttribute("method", method);
	return form;
}

/* DIV 생성 */
function createDIV(objId, className, funcName){
	const div = document.createElement("div");
	if(objId != '') div.setAttribute("id", objId);
	if(className != '') div.setAttribute("class", className);
	if(funcName != '') div.setAttribute("onClick", funcName);
	
	return div;
}

function createDiv(objId, className, funcName, innerText){
	const div = document.createElement("div");
	if(objId != '') div.setAttribute("id", objId);
	if(className != '') div.setAttribute("class", className);
	if(funcName != '') div.setAttribute("onClick", funcName);
	if(innerText != '') div.innerText = innerText;
	return div;
}
/* Input Box 생성*/
function createInputBox(type, name, value, placeholder){
	const input = document.createElement("input");
	input.setAttribute("type", type);
	input.setAttribute("name", name);
	if(value != "") input.setAttribute("value", value);
	if(placeholder != "") input.setAttribute("placeholder", placeholder);
	return input;
}
/* DatePicker 생성 */
function createDatePicker(name, minDate, maxDate){
	const date = document.createElement("input");
	date.setAttribute("type", "date");
	date.setAttribute("name", name);
	if(minDate != "") date.setAttribute("min", minDate);
	if(maxDate != "") date.setAttribute("max", maxDate);
	return date;	
} 

function createSelect(name, options, className, displayName){
	const select = document.createElement("select");
	select.setAttribute("name", name);
	select.setAttribute("class", className);
	
	if(displayName != null && displayName != ''){
		const option = document.createElement("option");
		option.innerText = displayName;
		select.appendChild(option);
	}
	
	for(idx=0; idx<options.length; idx++){
		const option = document.createElement("option");
		option.setAttribute("value", options[idx].levCode);
		option.innerText = options[idx].levName;
		select.appendChild(option);
	}
	return select;
}

function createFileBox(name){
	const fileBox = document.createElement("div");
	fileBox.setAttribute("class", "goods fileBox");
	const uploadName = document.createElement("input");
	uploadName.setAttribute("class", "uploadName");
	uploadName.setAttribute("readOnly", true);
	uploadName.setAttribute("placeholder", "첨부파일");
	uploadName.style.marginRight = "3%";
	const label = document.createElement("label");
	label.setAttribute("for", "file");
	label.innerText = "찾기";
	const file = document.createElement("input");
	file.setAttribute("type","file");
	file.setAttribute("id","file");
	file.setAttribute("name", name);
	file.addEventListener("change", function(){
		let fileName = document.getElementById("file").value;
		document.getElementsByClassName("uploadName")[0].value = fileName;
	});
	
	fileBox.appendChild(uploadName);
	fileBox.appendChild(label);
	fileBox.appendChild(file);
	
	return fileBox;
}

function accessOut(){
	const form = createForm("", "AccessOut", "post");
	document.body.appendChild(form);
	form.submit();
}
