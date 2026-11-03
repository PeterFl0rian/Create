# GitHub Actions

This document describes the GitHub Actions workflows in `.github/workflows/` and the other DevOps tools and services the project uses.

| Workflow | File | Trigger | Purpose |
|---|---|---|---|
| Build | `build.yml` | push, pull request, manual | Builds the mod, runs game tests, publishes releases |
| Validate | `validate.yml` | push, pull request, manual | Validates the Gradle wrapper and all JSON resources |
| Label Actions | `label-actions.yml` | issue / PR labeled or unlabeled | Comments on and closes issues and PRs based on labels |
| Crowdin Action | `localization.yml` | manual | Synchronizes translations with Crowdin |

## Build (`build.yml`)

The main CI workflow. It runs on every push and pull request, and can be started manually.

Steps:
1. Checks out the repository and switches to Java 21.
2. Sets up Gradle with `gradle/actions/setup-gradle`, which caches dependencies between runs.
3. Builds the mod with `./gradlew build`. This compiles the code and runs the `check` tasks, which include the JUnit unit tests and the JaCoCo coverage report.
4. Runs the in-game tests with `./gradlew runGameTestServer`. This starts a headless Minecraft server that runs Create's GameTests (`infrastructure/gametest`).
5. **Release mode only:** when started manually with `is-release: true`, it skips the game tests and runs `./gradlew publishMods`. The `mod-publish-plugin` then uploads the jar to Modrinth, CurseForge and GitHub Releases. It uses the `MODRINTH_TOKEN`, `CURSEFORGE_TOKEN` and `GITHUB_TOKEN` secrets, and the Gradle cache is disabled so the release is built from scratch.

## Validate (`validate.yml`) - new

Fast checks that do not need to build the mod. It runs on every push and pull request, and can be started manually. It has two independent jobs:

**Gradle Wrapper:** `gradle/actions/wrapper-validation` checks that `gradle/wrapper/gradle-wrapper.jar` matches the checksum of an official Gradle release. The wrapper jar runs on every developer machine and CI server that builds the project, so a tampered jar in a pull request would be a supply chain attack. This check catches it.

**JSON Resources:** `.github/scripts/validate_json.py` parses every `.json` file in `src/main/resources` and `src/generated/resources` (about 8000 files: translations, recipes, models, blockstates, tags, ...). It reports:
- invalid JSON (missing comma, trailing comma, unclosed bracket, ...)
- duplicate keys, e.g. the same translation key twice in a lang file, where one value silently overrides the other

Problems are reported as GitHub annotations, so they appear directly on the affected file in the pull request:
```
::error file=src/main/resources/assets/create/lang/de_de.json,line=120,col=5::Invalid JSON: Expecting ',' delimiter
Checked 8011 JSON files, found 1 problem(s).
```

This matters especially for translations, which arrive automatically from Crowdin (`localization.yml`), and for hand-edited resources. A broken JSON file would otherwise only show up when the game loads it.

Run the JSON check locally with:
```bash
python3 .github/scripts/validate_json.py src/main/resources src/generated/resources
```

Neither job uses Gradle or downloads dependencies, so the workflow finishes in seconds.

## Label Actions (`label-actions.yml`)

Automates issue and PR triage with [dessant/label-actions](https://github.com/dessant/label-actions). The action is pinned to an exact commit for security. When a maintainer adds a label, the workflow posts the matching comment from `.github/config/label-actions.yml`. Examples:
- `env: fabric`, `env: pojav`, `env: outdated create version`, ... close the issue as *not planned* and explain why (wrong mod loader, unsupported launcher, old version).
- `pr status: conflicts`, `pr status: waiting for author`, `pr status: wrong target` ask the PR author to resolve conflicts, apply requested changes, or retarget the branch.

It runs on `pull_request_target`, so it can comment on PRs from forks. It only has write permissions for issues, pull requests and discussions.

## Crowdin Action (`localization.yml`)

Synchronizes translations with [Crowdin](https://crowdin.com/), started manually:
1. Uploads the English source strings (`src/generated/resources/assets/create/lang/en_us.json`) to Crowdin.
2. Optionally uploads existing translations (`uploadTranslations` input).
3. Downloads the newest translations and opens a PR *New Crowdin translations* from the `l10n_crowdin_translations` branch into `mc1.21.1/dev`.

File and language mapping is configured in `crowdin.yml`. Credentials come from the `CROWDIN_PROJECT_ID` and `CROWDIN_PERSONAL_TOKEN` secrets.

## Other DevOps tools and services

| Tool / service | Where | What it does |
|---|---|---|
| Gradle (wrapper 8.14.3) + ModDevGradle | `build.gradle`, `gradlew` | Build tool. Sets up NeoForge and Minecraft, run configurations (client, server, data, game tests) and data generation. |
| Jenkins | `Jenkinsfile` | Second CI server run by the Create team. Builds the mod, publishes it to the `maven.createmod.net` Maven repository, archives the jars and sends the build result to Discord. |
| Maven repository | `publishing` in `build.gradle` | Publishes Create as a library for other mods (`maven.createmod.net`). |
| mod-publish-plugin | `publishMods` in `build.gradle` | Uploads releases to Modrinth, CurseForge and GitHub Releases, used by `build.yml`. |
| Silk | `build.gradle` | Generates the release changelog from `changelog.md`. |
| Blossom | `build.gradle` | Injects the mod version and git commit hash into the source code at build time. |
| Crowdin | `crowdin.yml`, `localization.yml` | Community translation platform. |
| GitHub issue templates | `.github/ISSUE_TEMPLATE/` | Structured forms for bug and crash reports. |
| JUnit 5, Mockito, JaCoCo | `build.gradle`, `src/test/` | Unit tests, test doubles and code coverage. Run as part of `./gradlew build` in `build.yml`, or locally with `./gradlew coverage`. |

## Known issue

At the time of writing, `maven.createmod.net` serves an expired SSL certificate. Gradle cannot download Create's dependencies from it, so `build.yml` fails during dependency resolution until the certificate is renewed. Locally, `./gradlew build --offline` works as long as the dependencies are already in the Gradle cache. `validate.yml` is not affected, because it does not use Gradle dependencies.
