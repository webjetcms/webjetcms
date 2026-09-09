# User-agent parsing rules

`regexes.yaml` is sourced from the Apache-2.0 licensed
[ua-parser/uap-core](https://github.com/ua-parser/uap-core) project. The application loads these bundled rules by default; the `uaParserYamlPath` configuration value can still override them.

Update the bundled rules explicitly from the latest `uap-core` `master` commit:

```shell
ant -f ant/build.xml updateUaParserRegexes
```

To reproduce or restore a specific upstream version, pass its full commit hash:

```shell
ant -f ant/build.xml -DuaParserRef=0123456789abcdef0123456789abcdef01234567 updateUaParserRegexes
```

The updater validates the downloaded file before replacing `regexes.yaml` and records the resolved upstream commit in `upstream-commit.txt`. Review and commit both files together.
