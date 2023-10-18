const { flatten, uniq } = require('lodash');
const fs = require('fs');

function callback(err) {
    if (err) throw err;
    console.error(err);
}
  
class WebjetModifyPlugin {

    constructor(options) {
        this.options = options;
    }

    apply(compiler) {
        compiler.hooks.emit.tap('WebjetModifyPlugin', compilation => {
            this.getJsChunks(compilation);
        });
    }

    /**
     * Vytvori jsp kopiu subor js v [dist/js]. 
     * @param {string} chunk cesta k webpack chunku 
     * @param {object} assets mapa chunk kodu 
     */
    createJsp(chunk, assets) {
        const fileName = chunk.split('/').pop() + '.jsp';
        let file = this.WebjetJspHeader;

        if (!fs.existsSync('dist/js')) {
            fs.mkdir('dist/js', { recursive: true }, () => {
                this.formatFileByProcess(file, assets, fileName);
            });
        } else {
            this.formatFileByProcess(file, assets, fileName);
        }
    }

    /**
     * Zapise subory podla procesu buildenia app.
     */
    formatFileByProcess(file, assets, fileName) {
        if (process.env.npm_lifecycle_event === 'prod') {
            var code = JSON.parse(JSON.stringify(assets)).source;
            code = code.replace(/\${/gi, "\\${");
            code = code.replace(/#{/gi, "\\#{");
            fs.writeFileSync(`dist/js/${fileName}`, file += code, callback);
        } else if (process.env.npm_lifecycle_event === 'dev') {
            //do JSP suboru musime escapnut ${ za \${ a tiez #{
            var code = JSON.parse(JSON.stringify(assets)).source.children[0]._value;
            code = code.replace(/\${/gi, "\\${");
            code = code.replace(/#{/gi, "\\#{");
            fs.writeFileSync(`dist/js/${fileName}`, file += code, callback);
        }
    }

    /**
     * Zkontroluje ci sa jedna o javascript subor ak ano zavola createJsp.
     * @param {object} compilation object ktory sa vytvara pocas toho ako webpack kompiluje data. 
     */
    getJsChunks(compilation) {
        this.getAllChunks(compilation).forEach(chunk => {
            const chunkExtension = this.getChunkExtension(chunk);
            if (chunkExtension === 'js' && this.controlOptions(chunk)) {
                const assets = compilation.getAsset(chunk);
                this.createJsp(chunk, assets);
            }
        });
    }

    /**
     * Zkontroluje ci je nazov suboru zhodny s nazvom v options.
     * @param {string} chunk  
     */
    controlOptions(chunk) {
        const name = chunk.split('/').pop().split('.')[0];
        if (!name) {
            throw Error('WebjetModifyPlugin: Zkontrolujte ci sa nezmenilo "vytvaranie nazvov" na riadku 76.')
        }
        return this.options.some(option => option === name);
    }

    /**
     * Vrati path k vsetkym webpack chunkom a mapam.
     * @param {object} compilation object ktory sa vytvara pocas toho ako webpack kompiluje data.
     */
    getAllChunks(compilation) {
        const jsonCompilation = compilation.getStats().toJson();
        return uniq(flatten(jsonCompilation.chunks.map(chunk => chunk.files))).reverse();
    }

    /**
     * Vrati koncovku suboru.
     * @param {string} chunk nazov suboru 
     */
    getChunkExtension(chunk) {
        const splittedChunk = chunk.split('.');
        return splittedChunk[splittedChunk.length - 1];
    }

    get WebjetJspHeader() {
        return `
        <%@ page pageEncoding="utf-8" contentType="text/javascript" %>
        <%@ page import="sk.iway.iwcm.Constants"%>
        <%@ page import="sk.iway.iwcm.Tools"%>
        <%@ page import="sk.iway.iwcm.i18n.Prop"%>
        <%  sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript"); 
            sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/dist/main.js", null, request, response);
        %>
        <%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" />
        `;
    }
};

module.exports = WebjetModifyPlugin;