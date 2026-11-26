// PR #386, merged as 4588dbc796. Use the DEV instance and the Slovak admin UI.
// Run from src/test/webapp with npm run video:plan, video:title, video or audio followed by this file.
Feature("video.386-58850-webjet-ai-pridat-podporu-pre-chat");

const videoPlan = {
    language: "sk",
    notes: "16:9. Keep the entire interface visible when windows change. Use brief details for text entry or an icon, then return to the full frame. Use the original PDF for the opening and fact check, retain the actual replies and append the full standard WebJET outro.",
    shots: [
        {
            id: "podklad", type: "manual", durationSeconds: 16, title: "The training brief",
            "text-sk": "Dostanete program školenia v PDF a potrebujete z neho pripraviť pozvánku na web. V podklade už máte termín, miesto, program aj cenu. Pozrite sa, ako z týchto údajov vytvoríte stránku priamo v editore WebJET CMS.",
            notes: "Show the single full page of video/assets/386-ai-cet/program-skolenia.pdf. Place the opening AI disclosure in the empty lower-right portion."
        },
        {
            id: "prvy-navrh", type: "auto", durationSeconds: 35, title: "From PDF to a first draft",
            "text-sk": "Začnite na uloženej stránke, pre ktorú pripravujete pozvánku. V nástrojovej lište pri obsahu stránky kliknite na ikonu AI a vyberte četového asistenta. Cez plus pridáte PDF s programom. Názov súboru vidíte pri správe. Teraz napíšte, na čo má text slúžiť. Požiadajte o pozvánku na web v HTML, aby ste ju mohli vložiť do editora. Asistentovi upresnite, že má zachovať údaje z prílohy. Odošlite správu. V odpovedi si môžete prečítať prvý návrh pozvánky.",
            notes: "Start in the classic content editor. Rest the cursor on the assistant name for two seconds before selecting it. Show the real attachment and the entire prompt before sending. Show the first answer during its narration and continue into the refinement on the next sentence.",
            prepare: async ({ openPage, DTE }) => {
                await openPage();
                // Reset only the unsaved editor buffer when recording another take.
                await DTE.fillCkeditor("<p>Tu pripravujeme pozvánku na školenie.</p>");
            },
            shot: async ({ I, openChat, attach, ask, firstPrompt, cue, latestPreview }) => {
                await cue("editor");
                await openChat(true);
                const sidebar = "#toast-container-ai .ai-chat-host__history";
                if (await I.grabNumberOfVisibleElements(sidebar)) {
                    await I.videoClick(sidebar + " .ai-chat-host__history-heading button:last-child");
                }
                await attach(true);
                await ask(firstPrompt, true);
                await I.waitForVisible(latestPreview(), 30);
                await cue("first-preview");
                await I.wait(5);
            }
        },
        {
            id: "spresnenie", type: "auto", durationSeconds: 43, title: "Refine, verify and save",
            "text-sk": "Úvod chcete skrátiť a na koniec pridať výzvu na registráciu. Napíšte to do toho istého rozhovoru. Program už druhýkrát prikladať nemusíte. Odošlite doplnenie a otvorte si väčší náhľad upravenej pozvánky. Pred vložením porovnajte údaje s podkladom. Školenie sa koná osemnásteho marca v ateliéri Lipa v Žiline. Skontrolujte aj časy jednotlivých tém, cenu štyridsaťpäť eur a adresu na prihlásenie. Zatvorte zväčšený náhľad. Pod upravenou odpoveďou kliknite na Nahradiť obsah. Zatvorte čet a pozrite si pozvánku priamo na stránke. Keď ste s textom spokojní, stránku uložte.",
            notes: "Expand the second preview, hold it beside the original PDF in the edit, then return to the last answer's Replace action. Hold the editor before saving.",
            prepare: async ({ ensureDraft }) => { await ensureDraft(); },
            shot: async ({ I, ask, refinement, cue, latestCard, readPreview, verifyFacts, host }) => {
                await ask(refinement, false);
                await I.videoClick(latestCard().find('button[aria-expanded="false"][aria-label="Maximalizovať okno"]'));
                await I.waitForVisible(host + " .ai-chat-html-expanded iframe", 20);
                verifyFacts(await readPreview());
                await cue("expanded");
                await I.wait(6);
                await I.videoClick(host + ' .ai-chat-html-expanded button[aria-expanded="true"]');
                await I.waitForInvisible(host + " .ai-chat-html-expanded", 10);
                await cue("return-to-answer");
                await I.videoClick(locate(host + ' .ai-chat-assistant [data-ai-action="replace"]').last());
                await I.waitForEnabled(host + ' [data-ai-action="undo"]', 20);
                await cue("inserted");
                await I.videoClick(host + " .toast-close-button");
                await I.waitForInvisible(host, 10);
                verifyFacts(await I.executeScript(() => window.ckEditorInstance.document.$.body.innerText));
                await cue("page-result");
                await I.wait(4);
                await I.videoClick("div.DTED.show .DTE_Footer button.btn-primary");
                await I.waitForInvisible("div.DTED.show", 30);
                await cue("saved");
            }
        },
        {
            id: "historia", type: "auto", durationSeconds: 21, title: "Return to the saved conversation",
            "text-sk": "Ak sa k pozvánke vrátite neskôr, otvorte tú istú stránku. Vidíte v nej uložený text. Pri jej obsahu znova otvorte AI čet. V histórii vyberte rozhovor o pozvánke. Nájdete v ňom pôvodné zadanie s prílohou aj následnú úpravu. Ak potrebujete ďalšiu zmenu, môžete pokračovať tu.",
            notes: "Reopen the exact saved page, then the data field chat. Open its actual thread and show both messages in sequence.",
            prepare: async ({ showPageRow }) => { await showPageRow(); },
            shot: async ({ I, openPageFromList, openChat, reopenThread, host, cue, verifyFacts }) => {
                await openPageFromList();
                verifyFacts(await I.executeScript(() => window.ckEditorInstance.document.$.body.innerText));
                await cue("reopened");
                await I.wait(3);
                await openChat(true);
                await reopenThread(true);
                const messages = locate(host + " .ai-chat-user");
                await I.moveCursorTo(host + " .toast-title");
                await I.scrollTo(messages.first());
                await I.waitForText("program-skolenia.pdf", 15, messages.first());
                await cue("old-prompt");
                await I.wait(3);
                await I.scrollTo(messages.last());
                await I.waitForText("Skráť", 15, messages.last());
                await I.waitForEnabled(host + " .ai-chat-input", 15);
                await cue("old-refinement");
                await I.wait(4);
            }
        },
        {
            id: "dokumentacia", type: "auto", durationSeconds: 10, title: "Chat documentation",
            "text-sk": "Ďalšie možnosti práce s prílohami a uloženými rozhovormi nájdete v dokumentácii WebJET CMS v časti AI Čet.",
            notes: "Hold on the Slovak AI Chat manual, then show the full brand animation after the narration.",
            shot: async ({ I, cue }) => {
                await I.amOnPage("https://docs.webjetcms.sk/latest/sk/redactor/ai/chat/");
                await I.waitForText("AI Čet", 25, "article h1");
                await cue("manual");
                await I.wait(5);
            }
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => { I.generateAudio(videoPlan); }).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    const { formatShotPlan } = require("../helpers/feature_video_plan.js");
    I.say(formatShotPlan(videoPlan));
});

function recordingContext({ I, DT, DTE }) {
    const assert = require("node:assert/strict");
    const fs = require("node:fs");
    const path = require("node:path");
    const output = path.resolve(__dirname, "../../../../docs/feature-video");
    const root = "/admin/v9/webpages/web-pages-list/";
    const title = "Ako písať pre web – pozvánka";
    const slug = "video-386-program-skolenia";
    const host = "#toast-container-ai";
    const firstPrompt = "Pozvánka na školenie pre web. Priprav ju z priloženého PDF po slovensky. Vráť len HTML pre editor: nadpis, krátky úvod, program a registráciu. Zachovaj presné názvy, termín, miesto, časy, cenu aj e-mail z podkladu.";
    const refinement = "Skráť úvod na jednu krátku vetu a zakonči pozvánku priamou výzvou na registráciu e-mailom. Zachovaj všetky fakty aj názvy bodov programu. Vráť celú upravenú pozvánku ako HTML.";
    const latestCard = () => locate(host + " .ai-chat-html-response").last();
    const latestPreview = () => latestCard().find(".ai-chat-html-preview iframe");
    const cue = async name => {
        fs.mkdirSync(output, { recursive: true });
        fs.appendFileSync(path.join(output, "386-cues.ndjson"), JSON.stringify({ run: process.pid, name, time: Date.now() }) + "\n");
    };
    const fixtureFile = path.join(output, "386-page.json");
    const instance = process.env.CODECEPT_URL || "http://iwcm.interway.sk";
    let docId;
    const showPageRow = async () => {
        await I.amOnPage(root + "?groupid=67");
        await DT.waitForLoader();
        await DT.filterEquals("title", title);
        await DT.waitForLoader();
    };
    const row = () => locate("#datatableInit tbody tr").withDescendant(locate("a").withText(title));
    const ready = async () => {
        await DTE.waitForEditor();
        await I.clickCss("#pills-dt-datatableInit-content-tab");
        await I.executeScript(async () => {
            const field = window.webpagesDatatable.EDITOR.field("data");
            await field.s.opts.wjeditorReadyPromise;
            if (field.s.opts.wjeditor.editingMode) await field.s.opts.wjeditor.switchEditingMode("", false);
        });
        await DTE.waitForCkeditor();
        await I.waitForVisible(".cke_button__aibutton", 20);
    };
    const openPageFromList = async () => {
        await I.videoClick(row().find("a").withText(title));
        await ready();
    };
    const openPage = async () => {
        await I.amOnPage(root + "?docid=" + docId);
        await ready();
    };
    const setup = async () => {
        await showPageRow();
        if (await I.grabNumberOfVisibleElements(row()) === 0) {
            await I.click(DT.btn.add_button);
            await DTE.waitForEditor();
            await DTE.fillField("title", title);
            await I.executeScript(slug => {
                const editor = window.webpagesDatatable.EDITOR;
                editor.field("generateUrlFromTitle").val(false);
                editor.field("editorVirtualPath").val(slug + ".html");
                editor.field("virtualPath").val("/test-stavov/" + slug + ".html");
                editor.field("available").val(false);
            }, slug);
            await ready();
            await DTE.fillCkeditor("<p>Tu pripravujeme pozvánku na školenie.</p>");
            await I.executeScript(() => window.webpagesDatatable.EDITOR.one("submitSuccess", (_event, json) => {
                window.video386CreatedId = json.data[0].docId;
            }));
            await DTE.save(undefined, true);
            const id = await I.executeScript(() => window.video386CreatedId);
            assert.ok(id > 0);
            fs.mkdirSync(output, { recursive: true });
            fs.writeFileSync(fixtureFile, JSON.stringify({ instance, id }));
            await showPageRow();
        }
        assert.equal(await I.grabNumberOfVisibleElements(row()), 1, "Expected exactly one dedicated video page");
        await I.click(row().find("a").withText(title));
        await ready();
        const fixture = await I.executeScript(() => {
            const page = window.webpagesDatatable.EDITOR.currentJson;
            return { id: page.docId, url: page.virtualPath, title: page.title, group: page.groupId, isDemo: /Tu pripravujeme pozvánku na školenie|kurz@example\.org/i.test(window.ckEditorInstance.getData()) };
        });
        assert.equal(fixture.title, title);
        const previous = fs.existsSync(fixtureFile) ? JSON.parse(fs.readFileSync(fixtureFile, "utf8")) : {};
        assert.equal(Number(fixture.group), 67);
        assert.ok((previous.instance === instance && previous.id === fixture.id) ||
            fixture.isDemo || fixture.url.endsWith("/" + slug + ".html"),
            "Refusing to record against an unrelated page " + JSON.stringify(fixture));
        fs.mkdirSync(output, { recursive: true });
        const threadId = previous.instance === instance && previous.id === fixture.id ? previous.threadId : undefined;
        fs.writeFileSync(fixtureFile, JSON.stringify({ instance, id: fixture.id, url: fixture.url, threadId }));
        docId = fixture.id;
        await I.say("Dedicated DEV page " + docId);
        await cue("fixture-ready");
    };
    const click = async (target, animated) => {
        if (animated) await I.videoClick(target);
        else await I.click(target);
    };
    const openChat = async animated => {
        await click(".cke_button__aibutton", animated);
        await I.waitForVisible(host + " .btn-ai-action", 15);
        const assistant = await I.executeScript(() => {
            const choices = window.webpagesDatatable.EDITOR.TABLE.DATA.fields.find(f => f.name === "data").ai
                .filter(a => a.action === "live_chat" && a.persistentChat && ["openai", "gemini"].includes(a.provider) && a.to === "data");
            return choices.find(a => a.description.includes("gpt-6.1-sol")) || choices[0];
        });
        assert.ok(assistant, "A configured persistent OpenAI or Gemini chat for page content is required");
        await cue("assistant-menu");
        const choice = locate(host + " .btn-ai-action").withText(assistant.description)
            .withChild(locate(".provider").withText(assistant.providerTitle));
        if (animated) {
            await I.moveCursorTo(choice);
            await I.wait(2);
        }
        await click(choice, animated);
        await I.waitForEnabled(host + " .btn-ai-chat-attach", 15);
        await cue("chat-open");
    };
    const attach = async animated => {
        const file = path.join(__dirname, "assets/386-ai-cet/program-skolenia.pdf");
        // Listen before clicking the visible plus. The native picker is outside the recording surface.
        let selected;
        await I.usePlaywrightTo("listen for the attachment picker", async ({ page }) => {
            selected = page.waitForEvent("filechooser").then(chooser => chooser.setFiles(file));
        });
        await click(host + " .btn-ai-chat-attach", animated);
        await selected;
        await I.waitForText("program-skolenia.pdf", 15, host + " .ai-chat-composer");
        await I.moveCursorTo(host + " .ai-chat-input");
        await cue("attached");
        await I.wait(2);
    };
    const ask = async (prompt, first) => {
        const input = host + " .ai-chat-input";
        const count = await I.grabNumberOfVisibleElements(host + " .ai-chat-assistant");
        await I.fillField(input, prompt);
        await cue(first ? "first-prompt" : "followup-prompt");
        await I.wait(4);
        await I.videoClick(host + " .btn-ai-chat-send");
        await cue(first ? "sent-first" : "sent-followup");
        await I.waitForFunction(([host, count]) => {
            const replies = document.querySelectorAll(host + " .ai-chat-assistant");
            return replies.length > count && !!replies[replies.length - 1].querySelector('[data-ai-action="replace"]');
        }, [host, count], 150);
        await I.waitForInvisible(host + " .btn-ai-chat-stop", 150);
        await I.waitForVisible(latestPreview(), 15);
        if (first) {
            const threadId = await I.executeScript(() => window.webpagesDatatable.EDITOR.editorAi.aiUserInterface.currentChat.chat.state.threadId);
            assert.ok(threadId, "The first answer must belong to a stored conversation");
            const fixture = JSON.parse(fs.readFileSync(fixtureFile, "utf8"));
            fs.writeFileSync(fixtureFile, JSON.stringify({ ...fixture, threadId }));
        }
    };
    const reopenThread = async (animated, optional = false) => {
        const panel = host + " .ai-chat-host__history";
        if (!await I.grabNumberOfVisibleElements(panel)) await click(host + ' button[aria-controls$="-history"]', animated);
        await I.waitForFunction(([panel]) => {
            const list = document.querySelector(panel + " .ai-chat-host__history-list");
            return list?.getAttribute("aria-busy") === "false" && list.childElementCount > 0;
        }, [panel], 20);
        if (animated && await I.grabNumberOfVisibleElements(panel + " .ai-chat-host__history-select") > 1) {
            const search = panel + ' input[type="search"]';
            if (!await I.grabNumberOfVisibleElements(search)) await I.videoClick(panel + ' button[aria-controls$="-history-filter"]');
            await I.usePlaywrightTo("find the thread by its follow-up", async ({ page }) => {
                await Promise.all([
                    page.waitForResponse(r => r.url().includes("/admin/rest/ai/chat/threads?") &&
                        new URL(r.url()).searchParams.get("query") === "Skráť úvod" && r.ok()),
                    page.locator(search).fill("Skráť úvod")
                ]);
            });
            await I.waitForVisible(panel + " .ai-chat-host__history-select", 15);
        }
        const fixture = JSON.parse(fs.readFileSync(fixtureFile, "utf8"));
        const threadIndex = await I.executeScript(id => {
            const items = window.webpagesDatatable.EDITOR.editorAi.aiUserInterface.currentChat.host.historyItems;
            return items.findIndex(item => id ? item.id === id : item.title.startsWith("Pozvánka na školenie pre web"));
        }, fixture.threadId || null);
        if (optional && threadIndex < 0) return false;
        assert.ok(threadIndex >= 0, "Record the PDF draft on this saved demo page before an isolated retake");
        const thread = locate(panel + " .ai-chat-host__history-select").at(threadIndex + 1);
        await I.waitForVisible(thread, 20);
        await cue("thread-list");
        await click(thread, animated);
        await I.waitForVisible(latestPreview(), 30);
        return true;
    };
    const ensureDraft = async () => {
        if (await I.grabNumberOfVisibleElements(host + " .ai-chat-assistant")) return;
        // Retakes reuse the real conversation of the saved fixture.
        await openPage();
        await openChat(false);
        await reopenThread(false);
    };
    const readPreview = () => I.executeScript(() => document.querySelector(".ai-chat-html-expanded iframe").contentDocument.body.innerText);
    const verifyFacts = text => {
        const normalized = text.replace(/\s+/gu, " ");
        for (const fact of [/ako písať pre web/i, /18\.\s*(?:marca|0?3\.)\s*2027/i, /ateliér\w* lipa/iu,
            /žilin/iu, /0?9[:.]00.{0,12}titulok a úvod/i, /10[:.]00.{0,12}úprava odsekov/i,
            /11[:.]00.{0,12}praktické cvičenie/i, /45\s*(?:€|eur)/i, /kurz@example\.org/i]) {
            assert.match(normalized, fact);
        }
    };
    return { host, setup, openPage, openPageFromList, showPageRow, openChat, attach, ask, firstPrompt, refinement,
        latestPreview, latestCard, readPreview, verifyFacts, ensureDraft, reopenThread, cue };
}

Scenario("YouTube thumbnail", async ({ I, DT, DTE, login }) => {
    if (process.argv.includes("dry-run")) return;
    const ctx = recordingContext({ I, DT, DTE });
    login("admin");
    await ctx.setup();
    await ctx.openChat(false);
    if (!await ctx.reopenThread(false, true)) {
        await ctx.attach(false);
        await ctx.ask(ctx.firstPrompt, true);
    }
    await I.click(ctx.latestCard().find('button[aria-label="Maximalizovať okno"]'));
    await I.waitForFunction(() => document.querySelector(".ai-chat-html-expanded iframe")?.contentDocument?.body?.innerText.includes("kurz@example.org"), [], 20);
    await I.moveCursorTo(ctx.host + " .toast-title");
    await I.videoTitle("Z PDF na web\npomocou AI", 64, "glow");
    await I.clickCss(ctx.host + " .toast-close-button");
    await DTE.cancel();
}).tag("@title");

Scenario("386-58850-webjet-ai-pridat-podporu-pre-chat", async ({ I, DT, DTE, login }) => {
    if (process.argv.includes("dry-run")) return;
    const { recordVideoPlan } = require("../helpers/feature_video_plan.js");
    const context = recordingContext({ I, DT, DTE });
    await recordVideoPlan(I, { plan: videoPlan, context, setup: async () => {
        login("admin");
        await context.setup();
    } });
}).tag("@video");
