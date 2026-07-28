# Publishing

Page copy for the CurseForge and Modrinth project pages. Nothing here is
compiled or shipped; it exists so the store listing has a source of truth in the
repo rather than living only in a web form.

| File | Use |
|---|---|
| [`summary.txt`](summary.txt) | Short summary line. 133 characters; both platforms cap at 150 |
| [`description.md`](description.md) | Full description body. Usable verbatim on both sites |
| [`upload-settings.md`](upload-settings.md) | Per-file settings for each of the four jars |
| [`categories-and-tags.md`](categories-and-tags.md) | Category and tag selections per platform |

Both files are identical on the `1.20.1` and `1.21.1` branches — the listing
covers both.

## Project logo

`common/src/main/resources/assets/mealmastery/icon.png` (256×256) is the
mod-list icon and is wired into `fabric.mod.json` (`icon`) and the Forge /
NeoForge TOMLs (`logoFile`). It can be reused as the project avatar on both
sites. CurseForge recommends 400×400 and Modrinth 512×512; 256×256 uploads fine
on both but will be upscaled in some views.

## Farmer's Delight naming

Farmer's Delight is MIT licensed (both vectorwing's original and MehVahdJukaar's
Refabricated fork). Neither project's README, LICENSE nor wiki states any policy
on addon naming or permission. No affiliation or endorsement is claimed anywhere
in this copy.
