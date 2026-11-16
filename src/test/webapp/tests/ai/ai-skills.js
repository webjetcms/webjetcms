Feature("ai.ai-skills");

Scenario("Save and reopen skill tool links", ({ I, login, DT, DTE }) => {
    login("admin");
    const table = "aiSkillsDataTable";
    const name = "skill-links-autotest-" + I.getRandomText();
    const instructionsTab = "#pills-dt-aiSkillsDataTable-instructions-tab";
    const drawer = "#aiSkillsDataTable_modal .reference-drawer";

    I.amOnPage("/admin/v9/settings/ai-skills/");
    DT.waitForLoader();
    DT.clickAddButton(table);
    DTE.waitForEditor(table);
    I.dontSeeElement("#pills-dt-aiSkillsDataTable-tools-tab");
    DTE.fillField("name", name);
    I.clickCss(instructionsTab);
    I.clickCss("#aiSkillsDataTable_modal .reference-toggle");
    I.fillField(drawer + " input", "ai_tools_preview");
    I.click(locate(drawer + " button").withText("ai_tools_preview"));
    I.seeInField("#DTE_Field_instructions", "@tool(ai_tools_preview)");
    DTE.save(table, true);

    DT.filterEquals("name", name);
    const skill = locate("#aiSkillsDataTable tbody a").withText(name);
    I.waitForElement(skill);
    I.click(skill);
    DTE.waitForEditor(table);
    I.clickCss(instructionsTab);
    I.seeInField("#DTE_Field_instructions", "@tool(ai_tools_preview)");
    I.fillField("#DTE_Field_instructions", "Instructions without tool links.");
    DTE.save(table, true);
    I.amOnPage("/admin/v9/settings/ai-skills/");
    DT.filterEquals("name", name);
    I.waitForElement(skill);
    I.click(skill);
    DTE.waitForEditor(table);
    I.clickCss(instructionsTab);
    I.seeInField("#DTE_Field_instructions", "Instructions without tool links.");
    DTE.cancel(table);
    DT.deleteAll(table);
});
