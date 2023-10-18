var classMagic = {};

// prida objektu novy class
classMagic.add = function (elm, newCl) {
	if (elm && newCl) {
		elm = this.fixElm(elm);
		for (var i = 0; i < elm.length; i++) {
			// ak uz dany objekt ma dany class, nepridame ho
			if (!classMagic.has(elm[i], newCl)) 
			{
				elm[i].className += (this.get(elm[i])) ? " " + newCl : newCl;
			}
			try
			{
				if (("required"==newCl || "invalid"==newCl) && (elm[i].type=="radio" || elm[i].type=="checkbox"))
				{					
					var newcl2 = newCl+elm[i].type;
					if (!classMagic.has(elm[i], newcl2)) 
					{
						elm[i].className += (this.get(elm[i])) ? " " + newcl2 : newcl2;
					}
				}
			}
			catch (e) {window.alert(e);}
		}
		return true;
	}
	return false;
};

// zisti, ci dany objekt ma priradeny dany class
classMagic.has = function (elm, cl) {
	if (elm && cl) {
		if (actCl = this.get(elm)) {
			for (var i = 0; i < actCl.length; i++) {
				if (actCl[i] == cl) {
					return true;
				}
			}
		}
	}
	return false;
}

// nahradi class(-y) objektu novym classom
classMagic.set = function (elm, newCl) {
	if (elm && newCl) {
		elm = this.fixElm(elm);
		for (var i = 0; i < elm.length; i++) {
			elm[i].className = newCl;
		}
		return true;
	}
	return false;
};

// nahradi stary class objektu novym classom
classMagic.replace = function (elm, newCl, oldCl) {
	if (elm && newCl && oldCl) {
		elm = this.fixElm(elm);
		for (var i = 0; i < elm.length; i++) {
			var cl;
			var replCl = "";
			if (cl = this.get(elm[i])) {
				for (var j = 0; j < cl.length; j++) {
					replCl += (j > 0) ? " " : "";
					replCl += (cl[j] == oldCl) ? newCl : cl[j];
				}
				elm[i].className = replCl;
			}
		}
		return true;
	}
	return false;
};

// odstrani stary class
classMagic.remove = function (elm, oldCl) {
	if (elm && oldCl) {
		elm = this.fixElm(elm);
		for (var i = 0; i < elm.length; i++) {
			var cl;
			var replCl = "";
			if (cl = this.get(elm[i])) {
				for (var j = 0; j < cl.length; j++) {
					replCl += (j > 0) ? " " : "";
					replCl += (cl[j] == oldCl) ? "" : cl[j];
				}
				elm[i].className = replCl;								
			}
			
			try
			{
				if (("required"==oldCl || "invalid"==oldCl) && (elm[i].type=="radio" || elm[i].type=="checkbox"))
				{
					var oldcl2 = oldCl+elm[i].type;
					this.remove(elm[i], oldcl2);
				}
			}
			catch (e) {}
		}
		return true;
	}
	return false;
};

// vrati pole vsetkych classov, ktore objekt ma
// (toto je pomocna funkcia, ktoru vyuzivaju dalsie funkcie, ale moze sa pouzit aj samostatne)
classMagic.get = function (elm) {
	if (elm) {
		return (elm.className == "") ? false : elm.className.split(" ");
	}
	return false;
};

// pomocna funkcia, ktore prevedie samostatny element na pole
classMagic.fixElm = function (elm) {
	if (elm) {
		if (!elm.sort) {
			elm = [elm];
		}
		return elm;
	}
	return false;
}