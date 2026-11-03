# GitHub Actions

This document describes the GitHub Actions workflows in `.github/workflows/` and the other DevOps tools and services the project uses.

| Workflow | File | Trigger | Purpose |
|---|---|---|---|
| Build | `build.yml` | push, pull request, manual | Builds the mod, runs game tests, publishes releases |
| Tests | `tests.yml` | push, pull request, manual | Runs unit tests and reports code coverage |
| Label Actions | `label-actions.yml` | issue / PR labeled or unlabeled | Comments on and closes issues and PRs based on labels |
| Crowdin Action | `localization.yml` | manual | Synchronizes translations with Crowdin |

## Build (`build.yml`)

The main CI workflow. It runs on every push and pull request, and can be started manually.

Steps:
1. Checks out the repository and switches to Java 21.
2. Sets up Gradle with `gradle/actions/setup-gradle`, which caches dependencies between runs.
3. Builds the mod with `./gradlew build` (compiles the code and runs the `check` tasks).
4. Runs the in-game tests with `./gradlew runGameTestServer`. This starts a headless Minecraft server that runs Create's GameTests (`infrastructure/gametest`).
5. **Release mode only:** when started manually with `is-release: true`, it skips the game tests and runs `./gradlew publishMods`. The `mod-publish-plugin` then uploads the jar to Modrinth, CurseForge and GitHub Releases. It uses the `MODRINTH_TOKEN`, `CURSEFORGE_TOKEN` and `GITHUB_TOKEN` secrets, and the Gradle cache is disabled so the release is built from scratch.

## Tests (`tests.yml`) - new

Runs the JUnit unit tests and measures code coverage with JaCoCo. It runs on every push and pull request, and can be started manually.

Steps:
1. Checks out the repository, switches to Java 21 and sets up Gradle (same setup as `build.yml`).
2. Runs `./gradlew coverage`, which runs the tests (JUnit 5 + Mockito) and generates the JaCoCo report. The workflow fails if any test fails.
3. Writes the test count and the coverage table into the job summary, so the results are visible on the run page without downloading anything:
   ```
   Tests: 36, passed: 36, failed: 0, skipped: 0
   Code coverage (JaCoCo)
                              instructions       lines              branches           methods
   StringHelper                 97.5% (118/121)    95.2%   (20/21)   100.0%   (12/12)    66.7%     (2/3)
   ...
   ```
4. Uploads the HTML test report and the JaCoCo coverage report as the `test-reports` artifact. Steps 3 and 4 also run when the tests fail, so failures can be inspected.

Run the same thing locally with:
```bash
./gradlew coverage
```

Unlike `build.yml`, this workflow does not start a Minecraft server, so it gives quick feedback on the unit tests alone.

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
| JUnit 5, Mockito, JaCoCo | `build.gradle`, `src/test/` | Unit tests, test doubles and code coverage, used by `tests.yml`. |

## Known issue

At the time of writing, `maven.createmod.net` serves an expired SSL certificate. Gradle cannot download Create's dependencies from it, so both `build.yml` and `tests.yml` fail during dependency resolution until the certificate is renewed. Locally, `./gradlew coverage --offline` works as long as the dependencies are already in the Gradle cache.
