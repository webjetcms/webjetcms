# Development Containers

[Development Containers](https://containers.dev) (devconatiners) je způsob vývoje v kontejnerech. GUI vývojového prostředí běží na vašem počítači, ale jeho `backend` a kompletní spouštění kódu v kontejnerech.

![](architecture-containers.png)

Aktuálně je podporováno ve [VS Code](https://code.visualstudio.com/docs/devcontainers/containers), připravována je podpora pro [IntelliJ](https://youtrack.jetbrains.com/issue/IDEA-292050).

## Výhody použití devcontainers

Hlavní výhody použití devcontainers jsou:
- Na počítači vyžaduje pouze instalaci Docker (nepotřebujete mít instalovanou Javu, NodeJS atd.).
- Zjednodušuje celkovou instalaci prostředí na počítači vývojáře.
- Unifikuje prostředí mezi vývojáři - v kontejneru je instalována přesná verze jevy, NodeJS a dalších nástrojů. na produkčním prostředí.

Je to tedy vhodné hlavně pro následující scénáře:
- Pracujete na více projektech, každý používá jinou verzi Java, NodeJS a je obtížné koordinovat verze na vašem počítači.
- Občas potřebujete pracovat na zastaralém projektu, kde se používají již nepodporované technologie a je těžké je držet na vašem počítači.
- Projekt potřebujete rychle spustit/vyzkoušet/ověřit/otestovat.

Samozřejmě vývoj má i nevýhody – běh v kontejneru je o něco pomalejší, hlavně práce se souborovým systémem. Instalace `node_modules` je výrazně pomalejší (ale provádíte ji typicky zřídka) a start WebJET CMS je pomalejší o cca 20%. Podobně `git commit/push` trvá několik sekund déle.

Po spuštění projektu v kontejneru jsou mapovány standardní HTTP porty 80,443,8080 na lokální počítač, takže spuštěný WebJET z kontejneru zobrazíte standardně ve vašem prohlížeči stejně, jako byste projekt spustili na vašem počítači.

Informaci o spuštění z kontejneru je vidět vlevo dole kde je modrý text `Dev Container: meno`.

![](browser.png)

## Používání devcontainers

Používání devcontainers je snadné, v IDE se umíte přepínat mezi prací lokálně a prací v kontejneru, souborový systém je sdílený.

<!-- tabs:start -->

#### **VS Code**

Pro VS Code je třeba instalovat rozšíření `ms-vscode-remote.remote-containers`. Po instalaci se vám v levém dolním rohu zobrazí modrá ikona `><` k přepnutí mezi lokálním vývojem a vývojem v kontejneru. Klepnutím se zobrazí možnost `Reopen in Container` pro otevření vývojového prostředí z kontejneru a naopak `Reopen Folder Locally` pro otevření lokální verze pokud jste v kontejneru.

V levé nástrojové liště se zobrazuje ikona `Remote Explorer` kde se vám zobrazí seznam kontejnerů, který můžete v případě potřeby smazat a vytvořit nově.

Po spuštění ověřte nainstalovaná rozšíření, měla by se vám zobrazit výzva k instalaci doporučených rozšíření. Zkontrolovat je můžete kliknutím v levé liště na `Extensions` a zadáním výrazu `@recommended` do vyhledávání. V sekci `Workspace recommendations` se vám zobrazí seznam doporučených rozšíření is možností jejich instalace.

![](extensions.png)

Při prvním otevření projektu se vám mohou zobrazit chyby kompilace `Mapper` tříd, stačí ale třídu otevřít, provést změnu (mezera, smazání mezery) a uložit soubor a chyba se opraví.

#### **IntelliJ**

Podpora pro [IntelliJ](https://youtrack.jetbrains.com/issue/IDEA-292050) je připravována, tato sekce bude doplněna později.

<!-- tabs:end -->

## Nastavení

Pro [optimální běh devcontainers](https://code.visualstudio.com/remote/advancedcontainers/improve-performance) je třeba provést nastavení Docker a dalších nástrojů.

<!-- tabs:start -->

#### **Windows**

Pro Windows nejsou potřebná speciální nastavení Docker pro urychlení běhu, můžete ale ověřit informace na výše uvedeném odkazu. Může ale vzniknout problém s rozdílným [nastavením konce řádků mezi Windows/Linux](https://code.visualstudio.com/docs/devcontainers/tips-and-tricks#_resolving-git-line-ending-issues-in-containers-resulting-in-many-modified-files).

#### **MacOS**

Pro MacOS je třeba optimalizovat nastavení Docker pro [rychlejší práci s diskem](https://www.docker.com/blog/speed-boost-achievement-unlocked-on-docker-desktop-4-6-for-mac/). Klepněte na ikonu Docker v menu liště a zvolte možnost `Settings`, v kartě `General` zvolte možnost `VirtioFS` v sekci `Choose file sharing implementation for your containers`. V kartě `Advanced` zvolte možnost `Allow the default Docker socket to be used` a klepněte na tlačítko `Apply & Restart`.

![](docker-settings.png)

<!-- tabs:end -->

V kontejneru můžete použít DNS záznam `host.docker.internal` pro napojení se na váš počítač (např. na váš lokální databázový server).

## Gitlab/SSH klíče

Aby vám iv kontejneru fungovalo připojení na gitlab server s použitím SSH klíčů je třeba nastavit [sdílení klíčů](https://code.visualstudio.com/remote/advancedcontainers/sharing-git-credentials) pomocí `ssh-agent` mezi vaším počítačem a kontejnerem. Technicky byste mohli zkopírovat přímo SSH klíč do složky `/home/vscode/.ssh`, to ale není ideální řešení.

Nejprve je třeba spustit ssh-agent (jednorázová operace):

<!-- tabs:start -->

#### **Windows**

Spusťte `local Administrator PowerShell` a zadejte následující příkazy:

```sh
# Make sure you're running as an Administrator
Set-Service ssh-agent -StartupType Automatic
Start-Service ssh-agent
Get-Service ssh-agent
```

#### **MacOS**

V MacOS je SSH agent spuštěn standardně, mělo by stačit přidat SSH klíče jak je uvedeno níže.

<!-- tabs:end -->

A následně přidejte vaše SSH klíče do `ssh-agent` příkazem:

```sh
ssh-add $HOME/.ssh/id_rsa
```

Seznam přidaných klíčů zobrazíte příkazem:

```sh
ssh-add -l
```

To můžete následně ověřit iv terminálu kontejneru.

Po restartu devcontainers by vám mělo fungovat připojení na git/gitlab stejně jako na vašem počítači. Uvedená nastavení jsou také ve skriptu `.devcontainer/localInit.sh`, který se provede před každým spuštěním kontejneru, takže tyto příkazy nepotřebujete provádět manuálně.

Pokud byste měli problém s připojením na git server, vždy se můžete přepnout na lokální verzi a `pull/push` operaci provést lokálně.

## Spuštění prohlížeče automatizovaných testů

Pokud potřebujete vidět prohlížeč pro automatizované testy je třeba nastavit [přeposílání XServer/X11](https://www.oddbird.net/2022/11/30/headed-playwright-in-docker/) (manažer oken pro Linux) na váš počítač.

![](autotest.png)

<!-- tabs:start -->

#### **Windows**

Na Windows je `XServer` podporován vrámci `Windows Subsystem for Linux/WSL` a měl by být standardně dostupný na Windows 10 i 11. Pro ověření spusťte `WSL` přes start menu a zadejte následující příkaz:

```sh
ls -a -w 1 /mnt/wslg
```

ve výpisu byste měli vidět hodnotu `.X11-unix`. Pokud ne, přejděte do obchodu a stáhněte [aktuální verzi WSL](https://www.microsoft.com/store/productId/9P9TQF7MRM4R).

Bohužel v konfiguraci je rozdíl mezi Windows a MacOS, v souboru `.devcontainer/devcontainer.json` upravte hodnotu `"DISPLAY": "host.docker.internal:0"` na `"DISPLAY": ":0"`.

#### **MacOS**

Pro MacOS je nejprve třeba nainstalovat [XQuartz](https://www.xquartz.org). Po instalaci a restartu přejděte v aplikaci `XQuartz` do `Preferences -> Security` a zaškrtněte možnost `Allow connections from network clients`.

![](xquartz-settings.png)

Restartujte znovu počítač a následně zadejte v terminálu příkaz:

```sh
xhost +localhost
```

pro povolení připojení na `XQuartz` z lokálního počítače. Nastavení `xhost +localhost` je také ve skriptu `.devcontainer/localInit.sh`, který se provede před každým spuštěním kontejneru, abyste na to nezapomněli.

<!-- tabs:end -->

Po nastavení se vám bude zobrazovat okno z kontejnerizovaného Linux přímo na vašem počítači. Technicky umíte v tomto režimu spustit jakoukoli GUI aplikaci instalovanou v kontejneru.

Při prvním spuštění testů se možná zobrazí chyba, že není nainstalován `playwright`, v terminálu kontejneru spusťte příkazy pro jeho instalaci:

```sh
npx playwright install
npx playwright install-deps
```

## Ukázkové soubory

Následuje seznam ukázkových souborů, které můžete použít jako základ pro váš projekt. Všechny se nacházejí ve složce `.devcontainer`:

`devcontainer.json` - samotná konfigurace `devcontainer`:

```json
// For format details, see https://aka.ms/devcontainer.json. For config options, see the
// README at: https://github.com/devcontainers/templates/tree/main/src/alpine
{
	"name": "webjetcms-devcontainer",
	// Or use a Dockerfile or Docker Compose file. More info: https://containers.dev/guide/dockerfile
	"image": "mcr.microsoft.com/devcontainers/base:ubuntu",
	"features": {
		//https://github.com/devcontainers/features/tree/main/src/java
		"ghcr.io/devcontainers/features/java:1": {
			"version": "8",
			"jdkDistro": "open",
			"installAnt": true,
			"antVersion": "1.10.12"
		},
		//https://github.com/devcontainers/features/tree/main/src/node
		"ghcr.io/devcontainers/features/node:1": {
			"nodeGypDependencies": true,
			"version": "16"
		}
	},
    "runArgs": [
		//publish ports
		"--publish=80:80",
		"--publish=443:443",
		"--publish=8080:8080"
    ],

	"initializeCommand" : ".devcontainer/localInit.sh || true",
	"postCreateCommand": ".devcontainer/postCreateCommand.sh",
	"postStartCommand": ".devcontainer/postStartCommand.sh",

	//https://www.oddbird.net/2022/11/30/headed-playwright-in-docker/
	"containerEnv": {
        "DISPLAY": "host.docker.internal:0"
    },

	// Use 'forwardPorts' to make a list of ports inside the container available locally.
	//"forwardPorts": []

	// Configure tool-specific properties.
	"customizations": {
		"extensions": [
			"SonarSource.sonarlint-vscode",
			"vscjava.vscode-gradle",
			"naco-siren.gradle-language",
			"mutantdino.resourcemonitor",
			"srmeyers.git-prefix"
		],
		"settings": {
			"window.title": "${activeEditorMedium}${separator}${rootName}",
			"editor.stickyScroll.enabled": true,
			"java.debug.settings.showStaticVariables": true
		}
	}

	// Uncomment to connect as root instead. More info: https://aka.ms/dev-containers-non-root.
	// "remoteUser": "root"
}
```

`localInit.sh` - skript se spouští na lokálním počítači před vytvořením kontejneru:

```sh
#!/bin/sh

#enable MacOS Xquartz connection
xhost +localhost

#add local SSH key to ssh-agent
ssh-add $HOME/.ssh/id_rsa
ssh-add -l
```

`postCreateCommand.sh` - skript se spustí po vytvoření kontejneru již v jeho nitru, umožňuje instalovat dodatečné programy:

```sh
#!/bin/sh
sudo apt update
sudo apt install iputils-ping
sudo apt -y install imagemagick
```

`postStartCommand.sh` - skript se provede při každém spuštění kontejneru, z důvodu pomalého překladu `.local` domén přidává do `/etc/hosts` potřebné DNS záznamy:

```sh
#!/bin/sh

#set local DNS
sudo -- sh -c "echo '#LOCAL DNS FOR WEBJET' >> /etc/hosts"
sudo -- sh -c "echo '127.0.0.1    iwcm.interway.sk iwcm.iway.sk cms.iway.sk docs.interway.sk' >> /etc/hosts"
sudo -- sh -c "echo '1.2.3.4   gitlab.web.iway.local' >> /etc/hosts"
```
