console.log("this is script file")

const toggleSidebar=()=>{
if($(".sidebar").is(":visible")){
//close the sidebar
$(".sidebar").css("display","none");
$(".content").css("margin-left","0%");
}
else{
//open the sidebar
$(".sidebar").css("display","block");
$(".content").css("margin-left","20%");
}
};