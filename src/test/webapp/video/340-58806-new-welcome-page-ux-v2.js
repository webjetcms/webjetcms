Feature("video.340-58806-new-welcome-page-ux-v2");

const videoPlan = {
    language: "sk",
    notes: "Record the two demo profiles and the owner's mailbox separately. Edit on the spoken cues. Show the opening AI disclosure for eight seconds and append the entire WebJET outro after the closing hold.",
    shots: [
        {
            id: "upozornenie", type: "auto", durationSeconds: 38, title: "Recognize a new browser",
            "text-sk": "Keď sa prihlásite do WebJET CMS z nového prehliadača, ako zistíte, či je váš účet v bezpečí? Na úvodnej obrazovke si všimnite upozornenie Prihlásili ste sa z nového prehliadača. V schránke nájdete aj správu Nové prihlásenie do WebJET CMS. Skontrolujte názov prehliadača, operačný systém, IP adresu a čas. Zodpovedajú tomu, kedy a odkiaľ ste sa prihlásili? WebJET CMS si pamätá prehliadač spolu s jeho profilom. Aj na vlastnom počítači preto môžete dostať upozornenie, keď použijete nový profil alebo anonymné okno.",
            prepare: async ({ demo }) => { await demo.loginForm(); },
            shot: async ({ demo }) => {
                await demo.signInOwner();
                await demo.showNotice();
                await demo.showMail("owner", "alert");
            },
            notes: "Start on the owner's real login form. Hold the current-browser notice, then the delivered message with its device details."
        },
        {
            id: "potvrdenie", type: "auto", durationSeconds: 36, title: "Confirm your own sign-in with an email code",
            "text-sk": "Ak prihlásenie poznáte, vráťte sa k upozorneniu a kliknite na Bol som to ja. Pod upozornením sa otvorí pole na kód. V schránke otvorte novú správu s názvom Kód na potvrdenie prihlásenia do WebJET CMS. Šesťmiestny kód z tejto správy zadajte v administrácii a kliknite na Potvrdiť kód. Upozornenie zmizne. Výsledok si môžete overiť aj neskôr. Vpravo hore otvorte ponuku svojho účtu, vyberte Aktívne prihlásenia a potom Moje zariadenia. Pri svojom prehliadači uvidíte stav Potvrdené.",
            prepare: async ({ demo }) => { await demo.ensurePendingOwner(); },
            shot: async ({ demo }) => {
                await demo.requestCode();
                await demo.showMail("owner", "code");
                await demo.enterCode();
                await demo.showDevices("owner");
            },
            notes: "Retain the original dashboard tab while reading the code. Match the code, the disappearing notice and the confirmed badge to the same device."
        },
        {
            id: "blokovanie", type: "auto", durationSeconds: 45, title: "Block the other browser and verify sign-out",
            "text-sk": "Teraz sa do toho istého účtu prihlási druhý prehliadač. Na svojom prehliadači otvorte email o tomto ďalšom prihlásení. Ak údaje nepoznáte, kliknite v správe na Nebol som to ja – zabezpečiť účet. Otvorí sa okno Zabezpečte svoj účet. Máte ešte čas skontrolovať, ktorý prehliadač ste vybrali. Druhý prehliadač je zatiaľ stále prihlásený. Až keď kliknete na Zablokovať zariadenie, WebJET CMS ukončí jeho známe relácie. Vo vlastnom prehliadači zostávate prihlásení. V zozname Moje zariadenia uvidíte pri druhom prehliadači stav Zablokované. Keď sa v ňom pokúsite znovu otvoriť administráciu, zobrazí sa prihlasovací formulár.",
            prepare: async ({ demo }) => { await demo.ensureConfirmedOwner(); await demo.otherLoginForm(); },
            shot: async ({ demo }) => {
                await demo.signInOther();
                await demo.showMail("other", "alert");
                await demo.openReport();
                await demo.checkOtherStillSignedIn();
                await demo.blockOther();
                await demo.showDevices("other");
                await demo.checkOtherSignedOut();
            },
            notes: "Cut to the live second profile before the block, then back to the owner for the explicit action. Show the blocked row and the subsequent login form in the other profile."
        },
        {
            id: "overenie", type: "auto", durationSeconds: 42, title: "Try signing in from the blocked profile",
            "text-sk": "V tom istom zablokovanom profile skúste bežné prihlásenie so správnym menom a heslom. Zobrazí sa Overenie zablokovaného zariadenia. Na odblokovanie prehliadača a vstup do administrácie potrebujete kód, ktorý príde na email vášho účtu. Tento emailový kód sa líši od kódu z autentifikačnej aplikácie. Ak ste prihlásenie nespoznali, zmeňte si heslo, skontrolujte ostatné prihlásenia a zapnite si dvojstupňové overovanie. Aj po zablokovaní prehliadača môže niekto použiť prezradené heslo v inom profile. Podrobný postup nájdete v dokumentácii WebJET CMS v časti Ak prihlásenie nepoznáte.",
            prepare: async ({ demo }) => { await demo.ensureBlockedOther(); },
            shot: async ({ demo }) => {
                await demo.retryBlockedLogin();
                await demo.showAdvice();
                await demo.showDocumentation();
            },
            notes: "Keep the second profile and its cookies. Stop at the blank email-code field. Return to the owner's security advice and finish on the exact Slovak guide."
        }
    ]
};

Scenario("ElevenLabs", ({ I }) => {
    I.generateAudio(videoPlan);
}).tag("@audio");

Scenario("Shot plan", ({ I }) => {
    I.say(require("../helpers/feature_video_plan").formatShotPlan(videoPlan));
});

Scenario("YouTube thumbnail", async ({ I }) => {
    if (process.argv.includes("dry-run")) return;
    const { createDemo } = require("./assets/340-device/recording.cjs");
    const demo = await createDemo(I, true);
    try {
        await demo.loginForm();
        await demo.signInOwner();
        await demo.thumbnail();
        await I.videoTitle("Neznáme prihlásenie?\nZabezpečte si účet", 62, "glow");
        await demo.ensureConfirmedOwner();
    } finally {
        await demo.close();
    }
}).tag("@title");

Scenario("340-58806-new-welcome-page-ux-v2", async ({ I }) => {
    if (process.argv.includes("dry-run")) return;
    const { recordVideoPlan } = require("../helpers/feature_video_plan");
    const { createDemo } = require("./assets/340-device/recording.cjs");
    const demo = await createDemo(I);
    try {
        await recordVideoPlan(I, { plan: videoPlan, context: { demo } });
    } finally {
        await demo.close();
    }
}).tag("@video");
