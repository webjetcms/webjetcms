const fs = require('fs').promises;
const path = require('path');
const properties = require('properties');
const DocsifyToPdf = require('./node_modules/docsify-to-pdf/src/index.js');

const tmpDir = 'tmp';
const staticDir = 'static';
const tempSidebar = path.join(tmpDir, '_sidebar.md');
const tempIndex = path.join(tmpDir, 'index.html');
const newPage = '\n\n<div style="page-break-after: always;"></div>';
const rootNames = ['redactor', 'admin', 'sysadmin', 'developer', 'install'];
let root;

const config = {
	title: "WebJET CMS",
	pdfOptions: {
		format: 'A4',
		margin: { top: '80px', bottom: '80px'},
        displayHeaderFooter: true,
        headerTemplate: '',
        timeout: 0,
	},
	removeTemp: true,
	emulateMedia: "print",
	indexFile: `
		<!DOCTYPE html>
		<html lang="en">
		<head>
			<meta charset="UTF-8">
			<title>Document</title>
			<meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1" />
			<meta name="description" content="Description">
			<meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0">
			<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/docsify-themeable@0/dist/css/theme-simple.css">
			<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@tabler/icons-webfont@latest/tabler-icons.min.css">
			<link rel="stylesheet" href="style.css">
            <link rel="preconnect" href="https://fonts.googleapis.com">
            <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
            <link href="https://fonts.googleapis.com/css2?family=Open+Sans:ital,wght@0,300..800;1,300..800&display=swap" rel="stylesheet">
		</head>
		<body>
			<div class="logo">
				<img src="assets/images/logo-print.png" alt="logo" />
			</div>
			<div id="app"></div>
			<script>
				window.$docsify = {
					name: '',
					repo: '',
                    executeScript: true,
                    //routerMode: 'history',
                    //relativePath: false, //otherwise history routing will not work
                    markdown: {
                        renderer: {
                            link: function(href, title, text) {
                                let defaultLink = window.$docsify.markdown.renderer.origin.link(href, title, text);

                                //console.log("href=", href, "title=", title, "text=", text, "defaultLink=", defaultLink);
                                let fixed = false;
                                let doNotTouch = false;

                                if (href.startsWith('http') || href.startsWith('#')) {
                                    doNotTouch = true;
                                } else {
                                    let mainFolders = ['/developer/', '/install/', '/frontend/', '/custom-apps/', '/redactor/', '/admin/',  '/sysadmin/', '/deprecated/', '/CHANGELOG', '/ROADMAP'];
                                    //if href startsWith ../ and contains mainFolders remove all before mainFolders
                                    if (href.startsWith('../')) {
                                        for (let i = 0; i < mainFolders.length; i++) {
                                            let folder = mainFolders[i];
                                            if (href.includes(folder)) {
                                                let fixedHref = href.replace('.md', '');
                                                //remove all after hash
                                                fixedHref = fixedHref.split('#')[0];
                                                fixedHref = fixedHref.substring(fixedHref.indexOf(folder));

                                                href = 'DOC_BASE_HREF' + fixedHref;

                                                fixed = true;

                                                break;
                                            }
                                        }
                                    }

                                }

                                if (href.includes("../src/")) {
                                    href = "https://github.com/webjetcms/webjetcms/tree/main" + href.substring(href.indexOf("/src/"));
                                    fixed = true;
                                }

                                if (href.endsWith(".zip") || href.endsWith(".pdf") || href.endsWith(".docx") || href.endsWith(".xlsx") || href.endsWith(".pptx")) {
                                    href = 'DOC_BASE_HREF/DOC_MANUAL_HREF/' + href;
                                    fixed = true;
                                }

                                if (doNotTouch == false && fixed == false && defaultLink.includes('href="#/') && !defaultLink.includes('href="#/static/"') && !defaultLink.includes('href="#/static/"')) {
                                    const anchor = text
                                        .toLowerCase()
                                        .replace(/\\s+/g, '-')
                                        .replace(/-+/g, '-')
                                        .replace(/^-+|-+$/g, '');
                                    var fixedLink = window.$docsify.markdown.renderer.origin.link("#"+anchor, title, text)
                                    //console.log("fixedLink1=", fixedLink, "anchor=", anchor);
                                    return fixedLink;
                                }

                                if (fixed) {
                                    defaultLink = '<a href="' + href + '"';
                                    if (title) {
                                        defaultLink += ' title="' + title + '"';
                                    }
                                    defaultLink += ' >' + text + '</a>';
                                    //console.log("fixedLink2=", defaultLink);
                                }
                                return defaultLink;
                            }
                        }
                    }
				}
			</script>
			<!-- Docsify v4 -->
			<script src="//cdn.jsdelivr.net/npm/docsify@4"></script>
		</body>
		</html>
		`
};

// This object defines manual categories to generate in different languages.
// Each key (e.g., 'sk', 'en', 'cs') represents a language code, and the corresponding value is an array of manual names for that language.
let manualNames = {
    'sk' : ['admin', 'install', 'sysadmin', 'redactor'], //'redactor-Aplikácie', 'redactor-Používanie administrácie, Datatabuľky', 'redactor-Web stránky, Súbory' ],
    'en' : ['admin', 'install', 'sysadmin', 'redactor'], //'redactor-Applications', 'redactor-Using the administration, Datatables', 'redactor-Web pages, Files' ],
    'cs' : ['admin', 'install', 'sysadmin', 'redactor'], //'redactor-Aplikace', 'redactor-Použití administrace, Datové tabulky', 'redactor-Webové stránky, Soubory']
}

//manualNames = { 'cs' : ['sysadmin', 'redactor'] }

let version = "";

async function run() {
    const exitProgram = process.exit;
    process.exit = (code) => {
        if (code === 0) {
            return;
        } else {
            exitProgram(code);
        }
    };

    const languages = Object.keys(manualNames);
    for (let lng of languages) {

        await loadVersion(lng);

        //i need to have first letter lowercase Version -> version
        let versionLC = version.charAt(0).toLowerCase() + version.slice(1);
        //font and style only works inline
        let headerFooterStyle = "font-size: 7px; margin-left: auto; margin-right: 40px; text-align: right; display: block; background-color: red; font-family: 'Arial'"
        config.pdfOptions.headerTemplate = `<div style="${headerFooterStyle}">${config.title} ${versionLC}</div>`;
        config.pdfOptions.footerTemplate = `<div style="${headerFooterStyle}"><span class="pageNumber"></span>/<span class="totalPages"></span></div>`;

        for (let manualNamePath of manualNames[lng]) {
            let manualName;
            if (manualNamePath.includes('-')) {
                [manualName, sectionsToKeep] = manualNamePath.split('-');
            } else {
                manualName = manualNamePath;
                sectionsToKeep = null;
            }

            manualNamePath = manualNamePath.replace(/ /g, '-').replace(/,/g, '-').replace(/--/g, '-');
            //replace ľščťžýáíé to lsctzyaie
            manualNamePath = manualNamePath.normalize("NFD").replace(/[\u0300-\u036f]/g, "");
            manualNamePath = manualNamePath.toLowerCase();

            config.contents = [`${lng}/${manualName}/_sidebar.md`];
            config.pathToPublic = `${lng}/_media/manuals/webjetcms-${manualNamePath}.pdf`;
            const inputFile = `${lng}/${manualName}/_sidebar.md`;
            const listOfContentFile = `${lng}/${manualName}/listofcontent.md`;

            console.log(`Processing ${lng}/${manualName}`);

            try {
                console.log("Prepare input file:", inputFile);
                await prepareFiles(inputFile, listOfContentFile, sectionsToKeep, lng, manualName);
            } catch (error) {
                throw new Error(`Prepare File error ${error}`);
            }
            try {
                console.log("Process manual:", manualName);
                await processManual(manualName);
            } catch (error) {
                console.log(error);
                throw new Error(`Process manual error ${error}`);
            }
            finally {
                console.log("Cleanup files");
                await cleanupFiles(inputFile, listOfContentFile);
            }
        }

    }
    exitProgram(0);
}

run();

async function cleanupFiles(inputFile, listOfContentFile) {
    if (await fileExists(inputFile)) {
        await fs.unlink(inputFile);
    }
    if (await fileExists(listOfContentFile)) {
        await fs.unlink(listOfContentFile);
    }
    await fs.rename(tempSidebar, inputFile);
    await fs.rename(tempIndex, 'index.html');
    await fs.rm(tmpDir, { recursive: true, force: true });
}

async function prepareFiles(inputFile, listOfContentFile, sectionsToKeep, lng, manualName) {
    // Create tmpDir if it doesn't exist
    if (!(await fileExists(tmpDir))) {
        await fs.mkdir(tmpDir);
    }
    // Remove staticDir if it exists
    if (await fileExists(staticDir)) {
        await fs.rm(staticDir, { recursive: true, force: true });
    }
    // Move files to tmp file
    await fs.rename(inputFile, tempSidebar);
    await fs.rename('index.html', tempIndex);
    // Determine the root
    rootNames.forEach((rootName) => {
        if (inputFile.includes(rootName)) {
            root = rootName;
        }
    });
    const data = await fs.readFile(tempSidebar, 'utf8');
    let replacedData;

    if (root) {
        const rootRegex = new RegExp(`/${root}/`, 'g');
        replacedData = data.replace(rootRegex, (match) => lowerLevelPath(match));

        rootNames.filter((name) => name !== root).forEach((name) => {
            const othersRegex = new RegExp(`/${name}/`, 'g');
            replacedData = replacedData.replace(othersRegex, (match) => higherLevelPath(match));
        });
    } else {
        const linkRegex = /-\s*\[([^\]]+)\]\(\s*(\/[^\s]+)\s*\)/g;
        replacedData = data.replace(linkRegex, (match, linkText, filePath) => {
            const newPath = filePath.startsWith('/') ? filePath.slice(1) : filePath;
            return `- [${linkText}](${newPath})`;
        });
    }

    if (sectionsToKeep) {
        replacedData = await filterSections(replacedData, sectionsToKeep);
    }

    // Replace back link and JavaDoc
    const content = {'sk' : 'Obsah', 'en': 'Contents', 'cs' : 'Obsah'}
    replacedData = replacedData.replace(/^.*\(?\/\?back\)?.*$/gm, `<h1 class="non-numbered">${content[lng]}</h1>\n`);
    //insert version into <div class="sidebar-section">
    replacedData = replacedData.replace(/(<div class="sidebar-section">)(.*?)(<\/div>)/g, `$1$2<span class="version">${version}</span>$3`);

    replacedData = replacedData.replace(/^.*\[.*?\]\(.*?javadoc.*?\).*$/gm, '').replace('- JavaDoc', '');

    // Add List of Content link
    replacedData = '- [listofcontent](listofcontent.md)\n' + replacedData;
    await fs.writeFile(inputFile, replacedData, 'utf8');

    let listOfContentData = createListOfContentData(replacedData);
    listOfContentData = capitalizeSpecialTags(listOfContentData);
    listOfContentData = removeInnerParentheses(listOfContentData);

    //add JS code to fix YouTube videos
    //replacedData = replacedData.replace(/<iframe src="https:\/\/www.youtube.com\/embed\/([a-zA-Z0-9-]+)".*?<\/iframe>/g, '<div class="video-container"><iframe src="https://www.youtube.com/embed/$1" allowfullscreen></iframe></div>');
    listOfContentData += `

<script>
    //insert image before youtube iframe and set data attribute for CSS styling
    document.querySelectorAll('iframe').forEach((iframe) => {
        if (iframe.src.includes('youtube')) {
            //console.log(iframe.src);

            //parse video id from iframe src
            let videoId = iframe.src.split('/').pop();
            //console.log(videoId);

            //create P element with link to youtube
            const p = document.createElement('p');
            p.innerHTML = 'YouTube video: <a href="https://www.youtube.com/watch?v='+videoId+'">https://www.youtube.com/watch?v='+videoId+'</a>';
            p.classList.add('youtube-link');
            //append P element after iframe
            iframe.before(p);

            const img = document.createElement('img');
            img.src = "https://img.youtube.com/vi/"+videoId+"/hq720.jpg";
            img.classList.add('youtube-thumbnail');
            //append image before iframe
            iframe.before(img);
        }
    });
</script>

`

    await fs.writeFile(listOfContentFile, listOfContentData, 'utf8');
    // Write index.html

    let indexHtml = config.indexFile.trim();
    //replace 'https://docs.webjetcms.sk/latest/sk' with 'https://docs.webjetcms.sk/latest/lng'
    indexHtml = indexHtml.replace(/DOC_BASE_HREF/g, `https://docs.webjetcms.sk/latest/${lng}`);
    indexHtml = indexHtml.replace(/DOC_MANUAL_HREF/g, `${manualName}`);

    await fs.writeFile('index.html', indexHtml, 'utf8');
}

async function processManual() {
    // Execute PDF export
    console.log("\u001b[31m" + "Please wait for export and do not interrupt the terminal process... \u001b[0m");
    await suppressLogging(() => DocsifyToPdf(config));
    //await DocsifyToPdf(config);
    console.log(`\x1b[42m SUCCESS with file ${config.pathToPublic} \x1b[0m \n`);
}


// === Helpers ===

async function suppressLogging(asyncExecutableCode) {
    const originalConsoleLog = console.log;
    console.log = () => {};

    try {
        await asyncExecutableCode();
    } finally {
        console.log = originalConsoleLog;
    }
}

async function fileExists(filePath) {
    try {
        await fs.access(filePath);
        return true;
    } catch (error) {
        return false;
    }
}

function lowerLevelPath(filePath) {
    const dirPath = path.dirname(filePath);
    return dirPath.startsWith('/') ? dirPath.slice(1) : dirPath;
}

function higherLevelPath(filePath) {
    return `..${filePath.startsWith('/') ? '' : '/'}${filePath}`;
}

async function filterSections(data, sectionsToKeep) {
    const sections = sectionsToKeep.replace(/, /g, ',').split(',');
    const lines = data.split('\n');
    let result = [];
    let insideSection = false;
    let currentSection = '';

    for (const line of lines) {
        const sectionMatch = line.match(/^-\s+(.+)/);

        if (sectionMatch) {
            const sectionTitle = sectionMatch[1].trim();

            if (sections.includes(sectionTitle)) {
                insideSection = true;
                currentSection = sectionTitle;
                result.push(line);
            } else if (line.startsWith('- [:point_left:')){
                result.push(line);
            } else {
                insideSection = false;
            }
        } else if (insideSection) {
            if (/^-\s/.test(line)) {
                insideSection = false;
            } else {
                result.push(line);
            }
        } else if (line.startsWith('<div') || line.trim() === '') {
            result.push(line.replace(
                /(<div class="sidebar-section">)(.*?)(<\/div>)/,
                `$1${sectionsToKeep}$3`
            ));
        }
    }

    return result.join('\n');
}

function capitalizeSpecialTags(text) {
    return text.replace(/#([áčďéíľńóôŕšťúľž])([a-zA-Z0-9-]*)/g, (match, firstLetter, restOfTag) => {
      return `#${firstLetter.toUpperCase()}${restOfTag}`;
    });
}

function removeInnerParentheses(text) {
    return text.replace(/\(#([^()]+(?:\([^)]*\))?)\)/g, (match, p1) => {
        return `(#${p1.replace(/\(([^()]+)\)/g, '$1')})`;
    });
}

function createListOfContentData(replacedData) {
    let listOfContentData = `# ${config.title} \n`;
    listOfContentData += replacedData
        .replace('- [listofcontent](listofcontent.md)\n', '')
        .replace(/\[\s*([^\]]+?)\s*\]\(([^)]+)\)/g, (match, text) => {
            const anchor = text
                .toLowerCase()
                .replace(/\s+/g, '-')
                .replace(/-+/g, '-')
                .replace(/^-+|-+$/g, '');
            return `[${text}](#${anchor})`;
        });
    return listOfContentData + newPage;
}

async function loadVersion(lng) {
    //load build.properties which is Java properties file and parse it
    //example:
    //major.number=2024.0-SNAPSHOT
    //minor.number=033
    //build.date=20.01.2025 9\:39\:09

    const propertiesFile = await fs.readFile('../src/main/webapp/WEB-INF/build.properties', 'utf8');
    const parsedProperties = properties.parse(propertiesFile);

    const content = {'sk' : 'Verzia', 'en': 'Version', 'cs' : 'Verze'}

    version = `${content[lng]}: ${parsedProperties['major.number']}.${parsedProperties['minor.number']} ${parsedProperties['build.date']}`;
    console.log(version);
}