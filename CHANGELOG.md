# Changelog

## [v1.1.0](https://github.com/OsiriX-Foundation/karnak/tree/v1.1.0) (2025-03-21)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v1.0.3...v1.1.0)

**Implemented enhancements:**

- Email notifications fix: do not display rejected instances as errors [\#240](https://github.com/OsiriX-Foundation/karnak/issues/240)
- Improvements of profile element Add Action [\#237](https://github.com/OsiriX-Foundation/karnak/issues/237)
- New profile action : add tag [\#228](https://github.com/OsiriX-Foundation/karnak/issues/228)
- Develop a helper to generate the authorization header in the Destination STOW Form [\#227](https://github.com/OsiriX-Foundation/karnak/issues/227)
- Update DICOM's JSON reference files [\#221](https://github.com/OsiriX-Foundation/karnak/issues/221)
- New Profile Expression : ComputePatientAge\(\) [\#220](https://github.com/OsiriX-Foundation/karnak/issues/220)
- External Pseudonym tab : add delete all patients [\#213](https://github.com/OsiriX-Foundation/karnak/issues/213)
- New expression: call to an API endpoint  [\#209](https://github.com/OsiriX-Foundation/karnak/issues/209)
- Upgrade various dependencies [\#208](https://github.com/OsiriX-Foundation/karnak/issues/208)
- Upgrade Vaadin to version 24 [\#207](https://github.com/OsiriX-Foundation/karnak/issues/207)
- Springboot 3 migration [\#206](https://github.com/OsiriX-Foundation/karnak/issues/206)
- Clean pixel data profile: mask position issue when the same equipement produces different images sizes [\#179](https://github.com/OsiriX-Foundation/karnak/issues/179)
- Clean pixel data limitation [\#175](https://github.com/OsiriX-Foundation/karnak/issues/175)
- \[Clean Pixel\] Define exclusion rules in the UI [\#37](https://github.com/OsiriX-Foundation/karnak/issues/37)
- Feat/expression excludeinstance [\#236](https://github.com/OsiriX-Foundation/karnak/pull/236) ([mhenx](https://github.com/mhenx))
- Upgrade dependencies \(SpringBoot 3, Vaadin 24, ...\)+ new expression to call an API endpoint [\#210](https://github.com/OsiriX-Foundation/karnak/pull/210) ([jdcshug](https://github.com/jdcshug))

**Fixed bugs:**

- HTTP 405 error when accessing to the static images \(logo and spinner gif\) [\#243](https://github.com/OsiriX-Foundation/karnak/issues/243)
- Fix NPE loading spinner [\#224](https://github.com/OsiriX-Foundation/karnak/issues/224)
- Fix NPE loading spinner [\#223](https://github.com/OsiriX-Foundation/karnak/pull/223) ([jdcshug](https://github.com/jdcshug))

**Closed issues:**

- New Profile Expression Action : ExcludeInstance\(\) [\#235](https://github.com/OsiriX-Foundation/karnak/issues/235)
- Expression do not handle multiple string values of a DICOM tag [\#211](https://github.com/OsiriX-Foundation/karnak/issues/211)

**Merged pull requests:**

- Remove unecessary MVC configuration to fix static images serving [\#244](https://github.com/OsiriX-Foundation/karnak/pull/244) ([mhenx](https://github.com/mhenx))
- Unit tests for JSON standard files [\#242](https://github.com/OsiriX-Foundation/karnak/pull/242) ([mhenx](https://github.com/mhenx))
- Adds a differentiation between error and excluded instances in monitoring and email notifications [\#241](https://github.com/OsiriX-Foundation/karnak/pull/241) ([mhenx](https://github.com/mhenx))
- Feat/code cleaning [\#239](https://github.com/OsiriX-Foundation/karnak/pull/239) ([mhenx](https://github.com/mhenx))
- new profile element add private tag and validation improvements on add action [\#238](https://github.com/OsiriX-Foundation/karnak/pull/238) ([mhenx](https://github.com/mhenx))
- UI fixes: pseudonym mapping flex display and icons for DICOM tools [\#234](https://github.com/OsiriX-Foundation/karnak/pull/234) ([mhenx](https://github.com/mhenx))
- fix merge error ProfileTest [\#233](https://github.com/OsiriX-Foundation/karnak/pull/233) ([mhenx](https://github.com/mhenx))
- Clean Pixel Data: add conditions on the image size to masks definition and application [\#232](https://github.com/OsiriX-Foundation/karnak/pull/232) ([mhenx](https://github.com/mhenx))
- Apply masks & clean pixel to any type of non compliant image [\#231](https://github.com/OsiriX-Foundation/karnak/pull/231) ([mhenx](https://github.com/mhenx))
- New profile action : add tag [\#230](https://github.com/OsiriX-Foundation/karnak/pull/230) ([mhenx](https://github.com/mhenx))
- Remove unused import and braces [\#229](https://github.com/OsiriX-Foundation/karnak/pull/229) ([mhenx](https://github.com/mhenx))
- Dialog to help generate Authorization Headers in the Destination STOW Form [\#226](https://github.com/OsiriX-Foundation/karnak/pull/226) ([mhenx](https://github.com/mhenx))
- ExternalID : Delete selected and delete all buttons - bug fix [\#225](https://github.com/OsiriX-Foundation/karnak/pull/225) ([mhenx](https://github.com/mhenx))
- Update DICOM's JSON reference files from Innolitics [\#222](https://github.com/OsiriX-Foundation/karnak/pull/222) ([mhenx](https://github.com/mhenx))
- New profile expression : computePatientAge [\#219](https://github.com/OsiriX-Foundation/karnak/pull/219) ([mhenx](https://github.com/mhenx))
- Minor UI fixes [\#218](https://github.com/OsiriX-Foundation/karnak/pull/218) ([mhenx](https://github.com/mhenx))
- ExternalID : Delete selected and delete all buttons [\#217](https://github.com/OsiriX-Foundation/karnak/pull/217) ([mhenx](https://github.com/mhenx))
- build\(deps\): bump org.json:json from 20230227 to 20231013 [\#212](https://github.com/OsiriX-Foundation/karnak/pull/212) ([dependabot[bot]](https://github.com/apps/dependabot))
- Feat/shift by date [\#204](https://github.com/OsiriX-Foundation/karnak/pull/204) ([redwork321](https://github.com/redwork321))

## [v1.0.3](https://github.com/OsiriX-Foundation/karnak/tree/v1.0.3) (2023-03-31)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v1.0.2...v1.0.3)

**Implemented enhancements:**

-  Monitoring/notification: add modality + sop class uid fields [\#200](https://github.com/OsiriX-Foundation/karnak/issues/200)
- Fix: dynamic new sop class uid in dicom transfers  [\#199](https://github.com/OsiriX-Foundation/karnak/issues/199)
- Mail sender in parameter [\#198](https://github.com/OsiriX-Foundation/karnak/issues/198)

**Fixed bugs:**

- Concurrency issue: association error for some instances with DICOM \(dimse\) destination [\#202](https://github.com/OsiriX-Foundation/karnak/issues/202)

**Closed issues:**

- Check behaviour hazelcast multi instance [\#176](https://github.com/OsiriX-Foundation/karnak/issues/176)

**Merged pull requests:**

- Mail sender, fix transfers dicom, modality + sop class uid in monitoring/notification \#198 \#199 \#200 [\#201](https://github.com/OsiriX-Foundation/karnak/pull/201) ([jdcshug](https://github.com/jdcshug))

## [v1.0.2](https://github.com/OsiriX-Foundation/karnak/tree/v1.0.2) (2022-11-05)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v1.0.1...v1.0.2)

**Merged pull requests:**

- fix: remove duplicate [\#196](https://github.com/OsiriX-Foundation/karnak/pull/196) ([jdcshug](https://github.com/jdcshug))

## [v1.0.1](https://github.com/OsiriX-Foundation/karnak/tree/v1.0.1) (2022-03-25)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v1.0.0...v1.0.1)

## [v1.0.0](https://github.com/OsiriX-Foundation/karnak/tree/v1.0.0) (2022-03-05)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v0.9.9...v1.0.0)

**Implemented enhancements:**

- Add in pre destroy reset transfer in progress status for destinations [\#192](https://github.com/OsiriX-Foundation/karnak/issues/192)
- Add automatic refresh of gateway setup when multiple instances [\#190](https://github.com/OsiriX-Foundation/karnak/issues/190)
- Add project secret history [\#189](https://github.com/OsiriX-Foundation/karnak/issues/189)
- Create a monitoring csv export [\#188](https://github.com/OsiriX-Foundation/karnak/issues/188)
- Notification improvement [\#187](https://github.com/OsiriX-Foundation/karnak/issues/187)
- Create pseudonym mapping view [\#186](https://github.com/OsiriX-Foundation/karnak/issues/186)
- Create monitoring view [\#185](https://github.com/OsiriX-Foundation/karnak/issues/185)
- Vulnerability CVE-2021-42550 \(aka LOGBACK-1591\) [\#180](https://github.com/OsiriX-Foundation/karnak/issues/180)

**Fixed bugs:**

- Change httpClient from HTTP2 to HTTP1\_1 [\#194](https://github.com/OsiriX-Foundation/karnak/issues/194)
- Handle 409 http exception in order to not rethrow exception = means file is already in the destination [\#193](https://github.com/OsiriX-Foundation/karnak/issues/193)
- Upgrade version weasis-dicom-tools in order to fix "Go Away" exceptions [\#191](https://github.com/OsiriX-Foundation/karnak/issues/191)

**Merged pull requests:**

- fix: switching album issue [\#195](https://github.com/OsiriX-Foundation/karnak/pull/195) ([jdcshug](https://github.com/jdcshug))
- monitoring + notification + export + secret history + pseudonym mapping [\#184](https://github.com/OsiriX-Foundation/karnak/pull/184) ([jdcshug](https://github.com/jdcshug))

## [v0.9.9](https://github.com/OsiriX-Foundation/karnak/tree/v0.9.9) (2021-12-20)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v0.9.8...v0.9.9)

**Implemented enhancements:**

- Update weasis-dicom-tools to 5.24.2 \(native lib to 4.5.3\) [\#177](https://github.com/OsiriX-Foundation/karnak/issues/177)
- DeIdentification front: refactoring + behaviour activate/deactivate deidentification [\#174](https://github.com/OsiriX-Foundation/karnak/issues/174)
- Notification Front: default values [\#172](https://github.com/OsiriX-Foundation/karnak/issues/172)
- Karnak email address [\#159](https://github.com/OsiriX-Foundation/karnak/issues/159)
- Change configuration when sending [\#31](https://github.com/OsiriX-Foundation/karnak/issues/31)
- Feat/notification sender [\#178](https://github.com/OsiriX-Foundation/karnak/pull/178) ([redwork321](https://github.com/redwork321))
- Feat/deidentification activ deactiv front refactoring [\#173](https://github.com/OsiriX-Foundation/karnak/pull/173) ([jdcshug](https://github.com/jdcshug))
- feat: default values notification [\#171](https://github.com/OsiriX-Foundation/karnak/pull/171) ([jdcshug](https://github.com/jdcshug))

## [v0.9.8](https://github.com/OsiriX-Foundation/karnak/tree/v0.9.8) (2021-08-26)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v0.9.7...v0.9.8)

**Implemented enhancements:**

- Adjust clickable zone checkbox [\#169](https://github.com/OsiriX-Foundation/karnak/issues/169)
- Add profile version in projects views [\#167](https://github.com/OsiriX-Foundation/karnak/issues/167)
- Springboot/junit/liquibase versions upgrade  [\#165](https://github.com/OsiriX-Foundation/karnak/issues/165)
- Enable/disable destination buttons \(save/delete\) when transfer is in progress  [\#163](https://github.com/OsiriX-Foundation/karnak/issues/163)
- Destinations: loading spinner transfer activity [\#161](https://github.com/OsiriX-Foundation/karnak/issues/161)
- Switching in the KHEOPS album cannot be applied with the KEEP action on the study UID and / or the serial UID [\#156](https://github.com/OsiriX-Foundation/karnak/issues/156)
- Image transcoding with a specific transfer syntax [\#139](https://github.com/OsiriX-Foundation/karnak/issues/139)
- Inject an external id provider [\#88](https://github.com/OsiriX-Foundation/karnak/issues/88)
- Check that the expression does not corrupt the DICOM [\#74](https://github.com/OsiriX-Foundation/karnak/issues/74)
- Improve the notification module UI [\#42](https://github.com/OsiriX-Foundation/karnak/issues/42)
- Adjust clickable zone checkbox [\#168](https://github.com/OsiriX-Foundation/karnak/pull/168) ([jdcshug](https://github.com/jdcshug))
- feat: add profile version [\#166](https://github.com/OsiriX-Foundation/karnak/pull/166) ([jdcshug](https://github.com/jdcshug))
- Upgrade versions springboot/junit/liquibase [\#164](https://github.com/OsiriX-Foundation/karnak/pull/164) ([jdcshug](https://github.com/jdcshug))
- Enable/disable destination buttons \(save/delete\) when transfer is in progress [\#162](https://github.com/OsiriX-Foundation/karnak/pull/162) ([jdcshug](https://github.com/jdcshug))
- Activity transfer destination [\#160](https://github.com/OsiriX-Foundation/karnak/pull/160) ([jdcshug](https://github.com/jdcshug))

**Merged pull requests:**

- Bump vaadin-bom from 19.0.6 to 19.0.9 [\#158](https://github.com/OsiriX-Foundation/karnak/pull/158) ([dependabot[bot]](https://github.com/apps/dependabot))
- Fix hash uidkeep [\#157](https://github.com/OsiriX-Foundation/karnak/pull/157) ([nicolasvandooren](https://github.com/nicolasvandooren))
- feat: add condition clean pixel data [\#155](https://github.com/OsiriX-Foundation/karnak/pull/155) ([jdcshug](https://github.com/jdcshug))

## [v0.9.7](https://github.com/OsiriX-Foundation/karnak/tree/v0.9.7) (2021-06-11)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v0.9.6...v0.9.7)

**Implemented enhancements:**

- Remove use pseudonym as patient name button and use pseudonym for patient name [\#154](https://github.com/OsiriX-Foundation/karnak/issues/154)
- Check Issuer of Patient ID in destination [\#148](https://github.com/OsiriX-Foundation/karnak/issues/148)
- Load an external logback configuration at startup [\#128](https://github.com/OsiriX-Foundation/karnak/issues/128)
- Set the logging level at startup [\#119](https://github.com/OsiriX-Foundation/karnak/issues/119)
- Pattern for the Clinical log warning [\#85](https://github.com/OsiriX-Foundation/karnak/issues/85)
- Defacing CT [\#17](https://github.com/OsiriX-Foundation/karnak/issues/17)

**Fixed bugs:**

- The order of profile elements on the ui and when downloading a profile is not correct.  [\#146](https://github.com/OsiriX-Foundation/karnak/issues/146)
- Do not de-identify when multiple actions and with retired SOP Class UID [\#138](https://github.com/OsiriX-Foundation/karnak/issues/138)
- Fix endianness of added sequences [\#137](https://github.com/OsiriX-Foundation/karnak/issues/137)

**Closed issues:**

- Condition to activate a destination [\#101](https://github.com/OsiriX-Foundation/karnak/issues/101)

**Merged pull requests:**

- feat: destination [\#153](https://github.com/OsiriX-Foundation/karnak/pull/153) ([jdcshug](https://github.com/jdcshug))
- Check issuer [\#152](https://github.com/OsiriX-Foundation/karnak/pull/152) ([cicciu](https://github.com/cicciu))
- feat: activate/deactivate notification [\#150](https://github.com/OsiriX-Foundation/karnak/pull/150) ([jdcshug](https://github.com/jdcshug))
- Defacing [\#149](https://github.com/OsiriX-Foundation/karnak/pull/149) ([cicciu](https://github.com/cicciu))
- Condition activate destination [\#145](https://github.com/OsiriX-Foundation/karnak/pull/145) ([nicolasvandooren](https://github.com/nicolasvandooren))
- External logback [\#144](https://github.com/OsiriX-Foundation/karnak/pull/144) ([nicolasvandooren](https://github.com/nicolasvandooren))
- feat: add unit tests +  header native for springdoc [\#143](https://github.com/OsiriX-Foundation/karnak/pull/143) ([jdcshug](https://github.com/jdcshug))

## [v0.9.6](https://github.com/OsiriX-Foundation/karnak/tree/v0.9.6) (2021-05-07)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v0.9.5...v0.9.6)

**Implemented enhancements:**

- Modal windows for the element of dicom worklist [\#135](https://github.com/OsiriX-Foundation/karnak/issues/135)
- Bump vaadin.version from 17.0.10 to 19.0.5 [\#133](https://github.com/OsiriX-Foundation/karnak/issues/133)
- remove the checkbox Authorized SOPs [\#97](https://github.com/OsiriX-Foundation/karnak/issues/97)
- Raise an exception from the execute function of an action. [\#73](https://github.com/OsiriX-Foundation/karnak/issues/73)
- \[Clean Pixel\] Recompression issue [\#39](https://github.com/OsiriX-Foundation/karnak/issues/39)

**Fixed bugs:**

- Decompress all the images with DICOM output [\#136](https://github.com/OsiriX-Foundation/karnak/issues/136)
- STOW-RS exceptions when sending images from multiple sources concurrently [\#124](https://github.com/OsiriX-Foundation/karnak/issues/124)

**Merged pull requests:**

- feat: vaadin 19 [\#142](https://github.com/OsiriX-Foundation/karnak/pull/142) ([jdcshug](https://github.com/jdcshug))
- feat: modify Project labels + set placeholder ProfileDropDown [\#141](https://github.com/OsiriX-Foundation/karnak/pull/141) ([jdcshug](https://github.com/jdcshug))
- fix: resources handler spring mvc + configuration echo controller spr… [\#140](https://github.com/OsiriX-Foundation/karnak/pull/140) ([jdcshug](https://github.com/jdcshug))
- Feat/echo endpoint [\#130](https://github.com/OsiriX-Foundation/karnak/pull/130) ([jdcshug](https://github.com/jdcshug))
- feat: add service unit tests + coverage on new code + deactivate previous not working unit tests [\#129](https://github.com/OsiriX-Foundation/karnak/pull/129) ([jdcshug](https://github.com/jdcshug))
- Deidentification method [\#126](https://github.com/OsiriX-Foundation/karnak/pull/126) ([cicciu](https://github.com/cicciu))

## [v0.9.5](https://github.com/OsiriX-Foundation/karnak/tree/v0.9.5) (2021-04-16)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v0.9.4...v0.9.5)

**Implemented enhancements:**

- Implement open id connect [\#112](https://github.com/OsiriX-Foundation/karnak/issues/112)

**Fixed bugs:**

- Exception loading sessions from persistent storage [\#127](https://github.com/OsiriX-Foundation/karnak/issues/127)

**Closed issues:**

- Enabled or Disabled a node or a destination [\#27](https://github.com/OsiriX-Foundation/karnak/issues/27)

## [v0.9.4](https://github.com/OsiriX-Foundation/karnak/tree/v0.9.4) (2021-03-17)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v0.9.3.1...v0.9.4)

**Implemented enhancements:**

- Reorganization in the types of pseudonyms and improvement the management of pseudonyms [\#118](https://github.com/OsiriX-Foundation/karnak/issues/118)
- Display a unique message when we add multiple  same external pseudonym via the csv file [\#108](https://github.com/OsiriX-Foundation/karnak/issues/108)
- Linking external pseudonym to a project [\#107](https://github.com/OsiriX-Foundation/karnak/issues/107)
- Pagination to visualize the External Pseudonym view [\#106](https://github.com/OsiriX-Foundation/karnak/issues/106)

**Fixed bugs:**

- Manage exception if status code is not SUCCESSFUL for a dicom stow [\#117](https://github.com/OsiriX-Foundation/karnak/issues/117)
- Manage exception on parsing datetime [\#116](https://github.com/OsiriX-Foundation/karnak/issues/116)
- Incorrect trailing \(FFFE,E00D\) Item Delimitation Item in outgoing C-STORE RQs. [\#109](https://github.com/OsiriX-Foundation/karnak/issues/109)

**Merged pull requests:**

- feat: merge pom.xml [\#125](https://github.com/OsiriX-Foundation/karnak/pull/125) ([jdcshug](https://github.com/jdcshug))
- enable/disable destination [\#123](https://github.com/OsiriX-Foundation/karnak/pull/123) ([cicciu](https://github.com/cicciu))
- Use attribute [\#122](https://github.com/OsiriX-Foundation/karnak/pull/122) ([nicolasvandooren](https://github.com/nicolasvandooren))
- External pseudonym project [\#121](https://github.com/OsiriX-Foundation/karnak/pull/121) ([cicciu](https://github.com/cicciu))
- Separate cache mainzelliste [\#120](https://github.com/OsiriX-Foundation/karnak/pull/120) ([cicciu](https://github.com/cicciu))
- Duplicate extid unique msg [\#115](https://github.com/OsiriX-Foundation/karnak/pull/115) ([cicciu](https://github.com/cicciu))
- Pagination extid [\#114](https://github.com/OsiriX-Foundation/karnak/pull/114) ([cicciu](https://github.com/cicciu))

## [v0.9.3.1](https://github.com/OsiriX-Foundation/karnak/tree/v0.9.3.1) (2021-02-05)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/v0.9.3...v0.9.3.1)

**Fixed bugs:**

- Profile format is not correct when it exported [\#111](https://github.com/OsiriX-Foundation/karnak/issues/111)

## [v0.9.3](https://github.com/OsiriX-Foundation/karnak/tree/v0.9.3) (2021-02-01)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/0.9.2...v0.9.3)

**Implemented enhancements:**

- Pop up warning when regenerate Project secret [\#92](https://github.com/OsiriX-Foundation/karnak/issues/92)
- Simplify the pseudonym cache metadata [\#90](https://github.com/OsiriX-Foundation/karnak/issues/90)
- In the UI, split the pseudonym cache and mainzelliste [\#89](https://github.com/OsiriX-Foundation/karnak/issues/89)
- Pass the action logs to level TRACE [\#84](https://github.com/OsiriX-Foundation/karnak/issues/84)
- Remove all references to the secret KARNAK\_HMAC\_KEY [\#81](https://github.com/OsiriX-Foundation/karnak/issues/81)
- Add Instance Creation Date and Time attributes [\#79](https://github.com/OsiriX-Foundation/karnak/issues/79)
- Update help view [\#78](https://github.com/OsiriX-Foundation/karnak/issues/78)
- Add rolling log for clinical log [\#77](https://github.com/OsiriX-Foundation/karnak/issues/77)
- Remove a profile [\#76](https://github.com/OsiriX-Foundation/karnak/issues/76)
- Improve the performance of the logs [\#67](https://github.com/OsiriX-Foundation/karnak/issues/67)
- Show masks in the profiles page [\#61](https://github.com/OsiriX-Foundation/karnak/issues/61)
- Export a profile [\#60](https://github.com/OsiriX-Foundation/karnak/issues/60)
- Reformat the code with google-java-format [\#40](https://github.com/OsiriX-Foundation/karnak/issues/40)
- Upgrade of version [\#23](https://github.com/OsiriX-Foundation/karnak/issues/23)
- Use of the DICOM structure according to Information Object Definition \(IOD\) [\#11](https://github.com/OsiriX-Foundation/karnak/issues/11)

**Fixed bugs:**

-  Upload profile error on Chrome [\#93](https://github.com/OsiriX-Foundation/karnak/issues/93)
- Missing level in log file [\#68](https://github.com/OsiriX-Foundation/karnak/issues/68)
- Hazelcast ClassNotFoundException in docker environment [\#66](https://github.com/OsiriX-Foundation/karnak/issues/66)

**Closed issues:**

- Logs the following fields for each de-identified instance in clinical logs \(Serie UID, Instance UID, ...\) [\#83](https://github.com/OsiriX-Foundation/karnak/issues/83)
- Add mask view in profile view [\#71](https://github.com/OsiriX-Foundation/karnak/issues/71)
-  Creation of a website for the karnak doc [\#70](https://github.com/OsiriX-Foundation/karnak/issues/70)

**Merged pull requests:**

- Refactor structure [\#104](https://github.com/OsiriX-Foundation/karnak/pull/104) ([jdcshug](https://github.com/jdcshug))
- Upload csv [\#103](https://github.com/OsiriX-Foundation/karnak/pull/103) ([cicciu](https://github.com/cicciu))
- add\_idp\_keycloak: Handle Keycloak IDP [\#102](https://github.com/OsiriX-Foundation/karnak/pull/102) ([jdcshug](https://github.com/jdcshug))
- Refactor pseudonym to remove duplicate code [\#100](https://github.com/OsiriX-Foundation/karnak/pull/100) ([nicolasvandooren](https://github.com/nicolasvandooren))
- Simplify the pseudonym and split the cached pseudonym and mainzelliste [\#98](https://github.com/OsiriX-Foundation/karnak/pull/98) ([nicolasvandooren](https://github.com/nicolasvandooren))
- Login Spring Security [\#95](https://github.com/OsiriX-Foundation/karnak/pull/95) ([jdcshug](https://github.com/jdcshug))
- Choose an action depending the standard DICOM [\#94](https://github.com/OsiriX-Foundation/karnak/pull/94) ([nicolasvandooren](https://github.com/nicolasvandooren))
- Improve clinical logs [\#86](https://github.com/OsiriX-Foundation/karnak/pull/86) ([cicciu](https://github.com/cicciu))
- Remove profile [\#82](https://github.com/OsiriX-Foundation/karnak/pull/82) ([cicciu](https://github.com/cicciu))
- Export profile [\#72](https://github.com/OsiriX-Foundation/karnak/pull/72) ([cicciu](https://github.com/cicciu))
- Production cache embedded [\#69](https://github.com/OsiriX-Foundation/karnak/pull/69) ([nicolasvandooren](https://github.com/nicolasvandooren))

## [0.9.2](https://github.com/OsiriX-Foundation/karnak/tree/0.9.2) (2020-11-04)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/0.9.1...0.9.2)

**Implemented enhancements:**

-  Reduce information in clinical log [\#56](https://github.com/OsiriX-Foundation/karnak/issues/56)
- Using caching for the Mainzelliste pseudonym [\#53](https://github.com/OsiriX-Foundation/karnak/issues/53)
- Improve the performance of pseudonym caching [\#52](https://github.com/OsiriX-Foundation/karnak/issues/52)
- Optimisation in actions [\#45](https://github.com/OsiriX-Foundation/karnak/issues/45)
- Upgrade Spring and Vaadin [\#41](https://github.com/OsiriX-Foundation/karnak/issues/41)
- Use DICOM endpoint as URL [\#36](https://github.com/OsiriX-Foundation/karnak/issues/36)
- Warning No projects created [\#35](https://github.com/OsiriX-Foundation/karnak/issues/35)
- Option shift date, keep year, month or day [\#30](https://github.com/OsiriX-Foundation/karnak/issues/30)
- Expression in replace [\#28](https://github.com/OsiriX-Foundation/karnak/issues/28)
- UI navigation [\#24](https://github.com/OsiriX-Foundation/karnak/issues/24)
- Switching in different KHEOPS' album [\#16](https://github.com/OsiriX-Foundation/karnak/issues/16)
- Implement clean pixel data \(text\) [\#14](https://github.com/OsiriX-Foundation/karnak/issues/14)
- Import an external pseudonym [\#13](https://github.com/OsiriX-Foundation/karnak/issues/13)
- Terms/Conditions on login page [\#8](https://github.com/OsiriX-Foundation/karnak/issues/8)
- Mainzelliste caching [\#64](https://github.com/OsiriX-Foundation/karnak/pull/64) ([nicolasvandooren](https://github.com/nicolasvandooren))

**Fixed bugs:**

- Validator applied to null fields after adding a new project [\#59](https://github.com/OsiriX-Foundation/karnak/issues/59)
- The project grid doesn't update after updating a project [\#58](https://github.com/OsiriX-Foundation/karnak/issues/58)
- What happens with a null UID [\#57](https://github.com/OsiriX-Foundation/karnak/issues/57)
- Define an "all" log file size [\#55](https://github.com/OsiriX-Foundation/karnak/issues/55)
- Sequences are not fully removed with Action X [\#51](https://github.com/OsiriX-Foundation/karnak/issues/51)
- Image with Clean pixel data is not sent to a DICOMWeb destination [\#50](https://github.com/OsiriX-Foundation/karnak/issues/50)
- DICOM connection between the forward node and the destination is closed randomly [\#49](https://github.com/OsiriX-Foundation/karnak/issues/49)
- Applying masks doesn't work when sending simultaneously several dataset [\#48](https://github.com/OsiriX-Foundation/karnak/issues/48)
- Notification for a DICOM destination doesn't work [\#47](https://github.com/OsiriX-Foundation/karnak/issues/47)
- Notification is not consistent when UIDs are changed by de-identification [\#46](https://github.com/OsiriX-Foundation/karnak/issues/46)
- NPE when setting a string value to Binary VR [\#44](https://github.com/OsiriX-Foundation/karnak/issues/44)
- Propagation of the action in sequence [\#29](https://github.com/OsiriX-Foundation/karnak/issues/29)
- Changing parameters during sending [\#26](https://github.com/OsiriX-Foundation/karnak/issues/26)
- Update grid project [\#63](https://github.com/OsiriX-Foundation/karnak/pull/63) ([nicolasvandooren](https://github.com/nicolasvandooren))

**Closed issues:**

- Use the Date Parser dcm4che [\#34](https://github.com/OsiriX-Foundation/karnak/issues/34)
- Patient Name splitted [\#33](https://github.com/OsiriX-Foundation/karnak/issues/33)

**Merged pull requests:**

- Seq not fully removed [\#65](https://github.com/OsiriX-Foundation/karnak/pull/65) ([cicciu](https://github.com/cicciu))
- Logback config [\#62](https://github.com/OsiriX-Foundation/karnak/pull/62) ([cicciu](https://github.com/cicciu))
- Pseudonym caching  [\#54](https://github.com/OsiriX-Foundation/karnak/pull/54) ([nicolasvandooren](https://github.com/nicolasvandooren))

## [0.9.1](https://github.com/OsiriX-Foundation/karnak/tree/0.9.1) (2020-09-07)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/0.9.0...0.9.1)

**Implemented enhancements:**

- Verification and error checking on the structure of the yaml file containing the profile [\#25](https://github.com/OsiriX-Foundation/karnak/issues/25)
- Expression in option or profile [\#22](https://github.com/OsiriX-Foundation/karnak/issues/22)
- Refactor constructor of ProfileItem [\#21](https://github.com/OsiriX-Foundation/karnak/issues/21)
- Option with Date [\#19](https://github.com/OsiriX-Foundation/karnak/issues/19)
- Exception type for destination [\#4](https://github.com/OsiriX-Foundation/karnak/issues/4)

## [0.9.0](https://github.com/OsiriX-Foundation/karnak/tree/0.9.0) (2020-07-24)

[Full Changelog](https://github.com/OsiriX-Foundation/karnak/compare/a8c8ee12a688add4caef2097ce5cc7c115180fbb...0.9.0)

**Implemented enhancements:**

- Import customize profile [\#20](https://github.com/OsiriX-Foundation/karnak/issues/20)
- Structure Profile [\#12](https://github.com/OsiriX-Foundation/karnak/issues/12)
- Filter by SOP [\#10](https://github.com/OsiriX-Foundation/karnak/issues/10)
- Configuration of user/password Admin [\#7](https://github.com/OsiriX-Foundation/karnak/issues/7)
- Vaadin Upgrade to v16 [\#6](https://github.com/OsiriX-Foundation/karnak/issues/6)
- Select all filters [\#5](https://github.com/OsiriX-Foundation/karnak/issues/5)

**Merged pull requests:**

- Make the UUID valid [\#3](https://github.com/OsiriX-Foundation/karnak/pull/3) ([spalte](https://github.com/spalte))
- Scale suggestion [\#2](https://github.com/OsiriX-Foundation/karnak/pull/2) ([spalte](https://github.com/spalte))



\* *This Changelog was automatically generated by [github_changelog_generator](https://github.com/github-changelog-generator/github-changelog-generator)*
