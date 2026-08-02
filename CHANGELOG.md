# Changelog

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
