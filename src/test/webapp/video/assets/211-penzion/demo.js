// Fictional public content for the feature film. The CMS renders every search result.
const prefix = "/penzion-pod-lipou/";
const question = "Môžeme prísť so psom a nechať auto u vás?";
const pages = [
    { slug: "ubytovanie", title: "Ubytovanie", text: "V Penzióne Pod lipou vás čaká šesť dvojlôžkových izieb. Každá má vlastnú kúpeľňu. Izbu si môžete prevziať od 15.00. V deň odchodu ju, prosím, uvoľnite do 10.00." },
    { slug: "pobyt-so-zvieratami", title: "Pobyt so zvieratami", text: "So psom sa u nás môžete ubytovať po predchádzajúcej dohode. Za psa účtujeme 8 eur za noc. Pri rezervácii nám napíšte, že prídete so psom." },
    { slug: "doprava-a-parkovanie", title: "Doprava a parkovanie", text: "Auto môžete nechať na súkromnom parkovisku vo dvore penziónu. Parkovanie je pre ubytovaných hostí bezplatné. Vjazd do dvora nájdete vedľa hlavného vchodu." },
    { slug: "ranajky", title: "Raňajky", text: "Raňajky podávame v jedálni na prízemí od 7.30 do 9.30. V ponuke je pečivo, ovocie, jogurt a vajíčka. Raňajky sú zahrnuté v cene izby." }
];
const search = { slug: "informacie-pre-hosti", title: "Informácie pre hostí" };
const url = page => prefix + page.slug + ".html";
const header = '<header><span class="brand">Penzión Pod lipou</span><span>Informácie o pobyte</span></header>';
const body = (item, groupId) => header + '<main><h1>' + item.title + '</h1>' + (item === search
    ? '<p>Čo by ste chceli vedieť pred príchodom?</p>!INCLUDE(/components/search/search.jsp, rootGroup=' + groupId + ', searchType=semantic, answerAllowed=falseValue, semanticSearchMinSimilarity=0.1, semanticSearchMinResults=2, ragAnswerMinSimilarity=0.1, perpage=4, sForm=complete)!'
    : '<p>' + item.text + '</p><p><a href="' + url(search) + '">Späť na informácie pre hostí</a></p>') + '</main>';

const groupName = "Penzión Pod lipou – video 211";
const marker = "webjet-video-211-autotest";
const head = `<!-- webjet-video-211-autotest -->
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
*{box-sizing:border-box}body{color:#253832;background:#fbfaf6;font:18px/1.45 -apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}
header{padding:18px max(6vw,calc((100vw - 960px)/2));display:flex;align-items:baseline;justify-content:space-between;background:#24463b;color:white;white-space:nowrap}
header .brand{font:600 28px Georgia,serif}header span+span{font-size:15px}main{max-width:1008px;margin:auto;padding:30px 24px 80px}
h1{font:normal 44px/1.15 Georgia,serif;margin:0 0 12px}main>p{margin:10px 0 18px;max-width:54ch}a{color:#205841}form p{display:flex;gap:8px}
.smallSearchInput{flex:1;max-width:720px;background:white;border:1px solid #8f9c94;border-radius:5px;font:inherit;padding:10px 14px}
.smallSearchSubmit{background:#275641;color:white;border:none;border-radius:5px;font:600 17px system-ui;padding:10px 25px}
.searchResultsH1{font-size:30px;margin:24px 0 8px}.search>p{margin:14px 0 3px}.search dl{margin:0 0 18px}.search dd{font-size:14px;margin:3px 0;color:#506359}.totalResults{font-size:15px}
.rag-answer{border:1px solid #b5c9ba;background:#eef4ec;border-radius:8px;padding:16px 20px}.rag-answer h5{display:flex;align-items:center;gap:8px;font-size:18px;margin:0 0 7px}.rag-answer p{margin:5px 0}.rag-answer ul{margin:4px 0}
</style>`;
module.exports = { pages, search, question, url, body, groupName, marker, head };
