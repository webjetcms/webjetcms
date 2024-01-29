//from https://github.com/docsifyjs/docsify/issues/656#issuecomment-675011539

const fs = require('fs');
const path = require('path');
const xmlbuilder = require('xmlbuilder');

const url = "https://docs.webjetcms.sk";
const suffix = "/latest";

//Walker function to go through directory and subdirectories
var walk = function(dir, done) {
  var results = [];
  fs.readdir(dir, function(err, list) {
    if (err) return done(err);
    var pending = list.length;
    if (!pending) return done(null, results);
    list.forEach(function(file) {
      file = path.resolve(dir, file);

      fs.stat(file, function(err, stat) {

        if (stat && stat.isDirectory()) {
          walk(file, function(err, res) {
            results = results.concat(res);
            if (!--pending) done(null, results);
          });
        } else {
            if(path.extname(path.basename(file)) === ".md" && !path.basename(file).startsWith('_') && path.basename(file).indexOf('tests-todo')==-1){

                //console.log("dirname", __dirname);
                let cleanDir = path.dirname(file.replace(__dirname, suffix));

                if(cleanDir == '/'){
                    cleanDir = "";
                }

                console.log(cleanDir);

                let urlPath = url+cleanDir+"/"+path.basename(file).replace('.md',"");

                results.push({

                    // format the file to a valid URL
                    url: urlPath,

                    // Last modified time for google sitemap
                    lastModified: stat.ctime
                  });
            }

          if (!--pending) done(null, results);
        }
      });
    });
  });
};

walk('.', function(err, results){


    let feedObj = {
        urlset: {
            '@xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance",
            "@xmlns:image":"http://www.google.com/schemas/sitemap-image/1.1",
            "@xsi:schemaLocation":"http://www.sitemaps.org/schemas/sitemap/0.9 http://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd http://www.google.com/schemas/sitemap-image/1.1 http://www.google.com/schemas/sitemap-image/1.1/sitemap-image.xsd",
            "@xmlns":"http://www.sitemaps.org/schemas/sitemap/0.9",
            url:[]
        }
    }

    results.forEach((data, i)=>{
            feedObj.urlset.url.push({
                loc: data.url,
                lastmod: data.lastModified.toISOString()
            })
    })

    let sitemap = xmlbuilder.create(feedObj, { encoding: 'utf-8' });


    fs.writeFile("sitemap.xml",sitemap.end({ pretty: true}),function(err){
        console.log(err)
    })

})