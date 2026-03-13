// A11Y helper for CodeceptJS using axe-playwright
// based on: https://github.com/kobenguyent/codeceptjs-a11y-helper/blob/main/src/index.ts

const { Helper } = codeceptjs;
const assert = require("assert");
const { resolve } = require("path");
const { readFileSync } = require("fs");

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
        console.log("WJ: runA11yCheck, opts=", opts);

		const { checkA11y, injectAxe, getViolations } = await import("axe-playwright");
        const { CreateReport, createHtmlReport, Options } = await import('axe-html-reporter');
		const { Playwright, A11yHelper } = this.helpers;
		if (!Playwright) throw new Error("Accessibility Tests only support Playwright - Chromium at the moment.");

		const { page } = Playwright;
		const _opts = { ...defaultRunA11YOpts, ...A11yHelper.config, ...opts };
		fileName = `${Date.now().toString()}_${_opts.reportFileName}`;

		await injectAxe(page);

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
        const violations = await getViolations(page, context, axeOptions?.axeOptions)

        //console.log("violations=", violations);

        if (violations.length > 0) {
            await createHtmlReport({
                results: { violations },
                options,
            });
            //testResultDependsOnViolations(violations, skipFailures)
        } else console.log('There were no violations to save in report')

        //reporterWithOptions = new TerminalReporterV2(axeOptions?.verbose ?? false)

        return violations;
	}

	async _before() {
		allure = codeceptjs.container.plugins("allure");
	}

	async _failed(test) {
        console.log("_failed");
		this._handleArtifacts(test);
		if (allure) {
			await this._attachArtifacts(test);
		}
	}

	async _passed(test) {
        console.log("_passed");
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
}

module.exports = A11yHelper;