# GitHub Actions

Overview of the CI workflows in `.github/workflows/` and other DevOps tools used by the project.

| Workflow | File | Trigger | Purpose |
|---|---|---|---|
| Build | `build.yml` | push, PR, manual | Builds the mod, runs tests, publishes releases |
| Validate | `validate.yml` | push, PR, manual | Checks the Gradle wrapper and JSON resources |
| Label Actions | `label-actions.yml` | label added/removed | Auto-comments on and closes issues and PRs |
| Crowdin Action | `localization.yml` | manual | Syncs translations with Crowdin |

## Build (`build.yml`)
- `./gradlew build` compiles the mod and runs the unit tests (JUnit, JaCoCo coverage).
- `./gradlew runGameTestServer` runs the in-game GameTests on a headless server.
- With `is-release: true`, publishes the mod to Modrinth, CurseForge and GitHub Releases (`publishMods`).

## Validate (`validate.yml`) - new
Fast checks that don't build the mod:
- **Gradle Wrapper:** verifies that `gradle-wrapper.jar` is an official Gradle release, which protects against a tampered wrapper.
- **JSON Resources:** `.github/scripts/validate_json.py` checks ~8000 JSON files (translations, recipes, models, ...) for invalid syntax and duplicate keys. Errors show up as annotations on the file in the PR.

Run the JSON check locally with:
```bash
python3 .github/scripts/validate_json.py src/main/resources src/generated/resources
```

## Label Actions (`label-actions.yml`)
When a maintainer adds a label, posts a predefined comment from `.github/config/label-actions.yml`. Examples:
- `env: fabric` closes the issue and links to the Fabric port.
- `pr status: conflicts` asks the author to resolve merge conflicts.

## Crowdin Action (`localization.yml`)
Uploads English strings to Crowdin, downloads translations and opens a PR with them into `mc1.21.1/dev`.

## Other tools

| Tool | Purpose |
|---|---|
| Gradle + ModDevGradle | Build tool, NeoForge setup, run configurations |
| Jenkins (`Jenkinsfile`) | Builds and publishes to `maven.createmod.net`, notifies Discord |
| mod-publish-plugin | Releases to Modrinth, CurseForge and GitHub |
| Crowdin | Community translations |
| Silk, Blossom | Changelog generation, version injection |
| JUnit 5, Mockito, JaCoCo | Unit tests and code coverage (`./gradlew coverage`) |

## Known issue
`maven.createmod.net` currently has an expired SSL certificate, so `build.yml` fails while downloading dependencies. `validate.yml` is not affected.
