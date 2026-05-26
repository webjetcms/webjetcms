// A11Y helper for CodeceptJS using axe-playwright
// based on: https://github.com/kobenguyent/codeceptjs-a11y-helper/blob/main/src/index.ts

const { Helper } = codeceptjs;
import { resolve } from "path";
import { readFileSync } from "fs";
//see https://www.deque.com/axe/core-documentation/api-documentation/#parameters-axerun
const defaultAxeOptions = {
	runOnly: {
		type: "tag",
		values: [
			"wcag2a",
			"wcag2aa",
			//"wcag2aaa",
			"wcag21a",
			"wcag21aa",
			"wcag22aa",
			//"best-practice",
			//"wcag***",
			//"ACT",
			//"experimental",
			//"cat.*",
		],
	},
};

const defaultRunA11YOpts = {
	context: null,
	axeOptions: defaultAxeOptions,
	detailedReport: true,
	detailedReportOptions: { html: true },
	skipFailures: true,
	reporter: "html",
	outputDir: "output",
	reportFileName: "accessibility-audit.html",
};

let outputDir;
let fileName;
let allure;

class A11yHelper extends Helper {
	constructor(config) {
		super(config);
		outputDir = config.outputDir || codeceptjs.config.get().output;
	}

	/**
	 * Run a11y check
	 * @param  {Options} opts - The options data
	 */
	async runA11yCheck(opts) {
        //console.log("WJ: runA11yCheck, opts=", opts);

		const { checkA11y, injectAxe, getViolations } = await import("axe-playwright");
        const { CreateReport, createHtmlReport, Options } = await import('axe-html-reporter');
		const { Playwright, A11yHelper } = this.helpers;
		if (!Playwright) throw new Error("Accessibility Tests only support Playwright - Chromium at the moment.");

		const { page } = Playwright;
		const _opts = { ...defaultRunA11YOpts, ...A11yHelper.config, ...opts };
		fileName = `${Date.now().toString()}_${_opts.reportFileName}`;

        const context = _opts.context;
        const axeOptions = {
            axeOptions: _opts.axeOptions,
            detailedReport: _opts.detailedReport,
            detailedReportOptions: _opts.detailedReportOptions,
        };
        const skipFailures = _opts.skipFailures;
        const reporter = _opts.reporter;
        const options = {
            outputDir: _opts.outputDir,
            reportFileName: fileName,
        };

        // Traverse nested iframes based on whitespace-separated selector segments.
        // E.g. "iframe.cke_dialog_ui_iframe #editorComponent" will:
        //   1. Find iframe.cke_dialog_ui_iframe in the page and switch into its frame
        //   2. Find #editorComponent inside that frame; if it is also an iframe, switch into it
        // Non-iframe segments are used as the axe context within the current frame.
        let targetPage = page;
        let targetContext = context;
        if (context && typeof context === 'string') {
            const parts = context.trim().split(/\s+/);
            let currentPage = page;
            let resolved = false;

            for (let i = 0; i < parts.length; i++) {
                const selector = parts[i];
                const element = await currentPage.waitForSelector(selector, { timeout: 5000 }).catch(() => null);

                if (!element) {
                    console.log(`a11y: "${selector}" not found in current frame, using remaining selector as axe context`);
                    targetPage = currentPage;
                    targetContext = parts.slice(i).join(' ');
                    resolved = true;
                    break;
                }

                const tagName = await element.evaluate(el => el.tagName.toLowerCase());
                console.log(`a11y: "${selector}" => tagName="${tagName}"`);

                if (tagName === 'iframe') {
                    const frame = await element.contentFrame();
                    if (!frame) {
                        console.log(`a11y: contentFrame() is null for "${selector}", falling back`);
                        targetPage = currentPage;
                        targetContext = parts.slice(i).join(' ');
                        resolved = true;
                        break;
                    }
                    await frame.waitForLoadState('domcontentloaded');
                    currentPage = frame;
                    console.log(`a11y: switched into iframe "${selector}"`);
                    if (i === parts.length - 1) {
                        // Last part was an iframe — run axe on the whole frame
                        targetPage = currentPage;
                        targetContext = null;
                        resolved = true;
                    }
                } else {
                    // Non-iframe element: use this and remaining parts as axe context
                    targetPage = currentPage;
                    targetContext = parts.slice(i).join(' ');
                    resolved = true;
                    break;
                }
            }

            if (!resolved) {
                targetPage = currentPage;
                targetContext = null;
            }
        }

		//console.log("a11y: targetPage=", targetPage.url(), "targetContext=", targetContext);
		await injectAxe(targetPage);

        const violations = await getViolations(targetPage, targetContext, axeOptions?.axeOptions)

        //console.log("violations=", violations);

        if (violations.length > 0) {
            await createHtmlReport({
                results: { violations },
                options,
            });
        }

        return violations;
	}

	async _before() {
		allure = codeceptjs.container.plugins("allure");
	}

	async _failed(test) {
		this._handleArtifacts(test);
		if (allure) {
			await this._attachArtifacts(test);
		}
		this._cleanup();
	}

	async _passed(test) {
        //console.log("_passed");
		this._cleanup();
	}

	_handleArtifacts(test) {
		if (fileName && outputDir) {
			test.artifacts.a11yReports = resolve(outputDir, fileName);
		}
	}

	async _attachArtifacts(test) {
		const timeString = Date.now().toString();
		const FORMAT = "application/zip";

		for (const [key, value] of Object.entries(test.artifacts)) {
			if (key !== "screenshot") {
				if (value) {
					if (key === "browserLogs" || key === "webSocketLogs")
						allure.addAttachment(`${key}:`, readFileSync(value), "text/plain");
					// The trace of session would be named like trace_${sessionName} that's why we don't have exact key here
					if (key.includes("trace"))
						allure.addAttachment(`${test.title} (${timeString}) - ${key}`, readFileSync(value), FORMAT);
					if (key.toLowerCase().includes("a11y"))
						allure.addAttachment(`${test.title} (${timeString}) - ${key}`, readFileSync(value), "text/html");
				}
			}
		}
	}

	_cleanup() {
		fileName = null;
	}
}

export default A11yHelper;