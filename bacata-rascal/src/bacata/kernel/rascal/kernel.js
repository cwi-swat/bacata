define([
	'custom/salix'], function(){
  		return {onload: function(){
			$(document).ready(new Salix().start);
  		}
		};
});
