# Changelog

## [0.4.2](https://github.com/groundsgg/library-i18n/compare/v0.4.1...v0.4.2) (2026-10-15)


### Bug Fixes

* **chat:** separate system context with a vertical bar ([#14](https://github.com/groundsgg/library-i18n/issues/14)) ([c8a36b3](https://github.com/groundsgg/library-i18n/commit/c8a36b36c34c300a3b186e8ea8e4bfbc33488407))

## [0.4.1](https://github.com/groundsgg/library-i18n/compare/v0.4.0...v0.4.1) (2026-10-15)


### Bug Fixes

* **chat:** omit duplicate brand beside the system logo ([#12](https://github.com/groundsgg/library-i18n/issues/12)) ([2eeb98c](https://github.com/groundsgg/library-i18n/commit/2eeb98c49596acdd597065ce03d9ba0e63c7b731))

## [0.4.0](https://github.com/groundsgg/library-i18n/compare/v0.3.0...v0.4.0) (2026-10-14)


### Features

* **i18n:** centralize localized Minecraft chat layouts ([#10](https://github.com/groundsgg/library-i18n/issues/10)) ([bbbfd97](https://github.com/groundsgg/library-i18n/commit/bbbfd973993e4e76698e75c218b21370e4d43ca7))

## [0.3.0](https://github.com/groundsgg/library-i18n/compare/v0.2.0...v0.3.0) (2026-10-14)


### Features

* add BundleCheck and SupportedLocales for in-repo translations ([#8](https://github.com/groundsgg/library-i18n/issues/8)) ([602b1ab](https://github.com/groundsgg/library-i18n/commit/602b1abadb4953c1f4ceb75b415973c4e786db1b))

## [0.2.0](https://github.com/groundsgg/library-i18n/compare/v0.1.1...v0.2.0) (2026-08-02)


### ⚠ BREAKING CHANGES

* `send` prefixes its output. Consumers that already emit conversation lines through a bundle must construct with `prefix = Prefix.NONE` or switch those call sites to `sendUnprefixed`.

### Features

* prefix every system message with [Grounds] ([#6](https://github.com/groundsgg/library-i18n/issues/6)) ([616cfdd](https://github.com/groundsgg/library-i18n/commit/616cfddce33d56d1c811f5703df60d4acfd5daf0))

## [0.1.1](https://github.com/groundsgg/library-i18n/compare/v0.1.0...v0.1.1) (2026-07-30)


### Bug Fixes

* target Java 21 bytecode so plugins can consume it ([#4](https://github.com/groundsgg/library-i18n/issues/4)) ([15f501b](https://github.com/groundsgg/library-i18n/commit/15f501b166f6d2cfb6b3afd41d58577c67f287c5))

## 0.1.0 (2026-07-30)


### ⚠ BREAKING CHANGES

* the reusable crowdin-sync workflow and templates/ are gone. No plugin consumes them yet, so nothing breaks in practice.

### Features

* manage translations in self-hosted Weblate, not Crowdin ([#3](https://github.com/groundsgg/library-i18n/issues/3)) ([2f07de2](https://github.com/groundsgg/library-i18n/commit/2f07de2aaa72610124fafa2a5225927014d84b0a))
* message keys resolved into the player's own language ([9eaabde](https://github.com/groundsgg/library-i18n/commit/9eaabde9a9c28fd627cd01391e21d510a37bc0cd))
