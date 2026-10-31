# Public functional pages — asset register, 29/10/2026

## Current policy — AI/demo branch illustrations, 30/10/2026

The owner now requests branch AI/demo images to appear with the visible label “Ảnh minh họa”, without a new verification/approval workflow. `fragments/branch-media.html` renders the existing model URL across Admin, Public, Member and the homepage gallery, with load-error/empty-source fallbacks. No backend or database verification state exists in the application model or managed SQL schemas; none was added.

The five previously inspected Cloudinary resources with foreign gym signage remain excluded from Public/Member/gallery by resource path, including the old PIG GYM source for ID 1. This is an image-content exception, not branch approval or an allowlist for uploaded photos. Replacement uploads with new URLs automatically use the shared illustration presentation. Admin retains the old resources only as clearly labelled foreign-gym source previews for replacement. Current ID 1's UTEFIT illustration renders; IDs 2–5 still require replacement using the owner's files. Gallery currently features only IDs 5, 4, 3 according to the unchanged catalog service.

See repository-root `BRANCH_IMAGE_DEMO_REPORT.md` for the current five-ID table, source mapping, checks and limits. The historical notes below explain earlier policies; their requirement to suppress every unverified URL is superseded by this owner-requested demo policy. No claim is made that the new illustration is a photograph of actual UTEFit premises, and no new stock/AI images were acquired or generated in this task.

## New: group-training illustration

- `public-classes-editorial.jpg` (1200 px wide, 153862 B) and `public-classes-editorial-mobile.jpg` (640 px wide, 51183 B).
- Pavel Danilyuk, “A group exercise class with a personal trainer leading stretching exercises in a gym.”
- Source: https://www.pexels.com/photo/personal-trainer-hosting-a-class-6339323/
- Download: https://images.pexels.com/photos/6339323/pexels-photo-6339323.jpeg (Pexels width/compression variants, original composition preserved).
- Pexels License: https://www.pexels.com/license/ verified 29/10/2026. Free website use and modification permitted; do not imply endorsement or redistribute unaltered stock. Not CC0, not a photograph of UTEFit classes/staff/members.
- Used only in Guest classes hero; caption labels illustration and links photographer/source. No hotlinked image at runtime; 640 variant selected on mobile.

## Existing assets, reused unchanged

- `journey-personal.webp` / `journey-personal-mobile.webp`: Farkas Mario, Unsplash https://unsplash.com/photos/YwK5YfOkqww under https://unsplash.com/license — existing register `public-journey-assets.md`. Trainer-list hero illustration, never a trainer avatar or evidence of UTEFit staff. Existing footer photographer credit retained.
- `cinema-kettlebell-v2-mobile.webp`: generated photoreal product artwork, existing register `cinematic-assets-v2.md`; plans hero only, explicitly illustration, not a gift included with a plan. No new AI images generated, no approved V2 texture changed.
- Branch architectural linework and roster name tokens are original code-native placeholders, not facility/staff photographs. Branch ID/name/address remain actual model values. Visible missing/unverified-photo labels.

## Unverified live branch photos — not used in new Guest list/detail

`PublicCatalogService.toBranchView` maps `Branch.image_url` directly. Live GET /branches returns Cloudinary URLs below; source is not a CSS fallback or hardcoded static photograph. AdminBranchServiceImpl supports uploaded image URLs and manually entered URLs; there is no provenance/rights-verification field in BranchView. That does not prove which administrative action populated this database or establish copyright ownership.

| Rendered branch | Current model image URL |
|---|---|
| UTEFit - Chi nhánh 5 | https://res.cloudinary.com/tburyrz8/image/upload/v1793278254/utefit/branches/nwfgqyo7j8xabno1c737.jpg |
| UTEFit - Chi nhánh 4 | https://res.cloudinary.com/tburyrz8/image/upload/v1793278244/utefit/branches/zmlyompmz4ulmk1dtpau.jpg |
| UTEFit - Chi nhánh 3 | https://res.cloudinary.com/tburyrz8/image/upload/v1793278235/utefit/branches/qoqqti8jvlilah0wxewk.jpg |
| UTEFit - Chi nhánh 2 | https://res.cloudinary.com/tburyrz8/image/upload/v1793278226/utefit/branches/xamgjfsidfbc0rhyvhhi.jpg |
| UTEFit - Chi nhánh 1 | https://res.cloudinary.com/tburyrz8/image/upload/v1793278217/utefit/branches/nix4fskixwaxy3ou0gkj.jpg |

User and before screenshots identify S’Life/FITGYM signage inconsistent with displayed UTEFit names. Ownership, permission and actual location are NOT verified. Do not assert infringement or that these are UTEFit premises. Existing database and original image files are unchanged. The Public branch list/detail intentionally do not request these URLs, including authenticated renderings which keep their established theme and existing empty-media styling.

## Branch policy synchronization, 30/10/2026

Live audit by ID 1–5 confirms all source URLs return HTTP 200 for Guest, Member and Admin. Signage visible in the original images: ID 1 PIG GYM; ID 2 S’LIFE GYM; ID 3 THE NEW GYM; ID 4 FITIGYM; ID 5 S’LIFE GYM. Cloudinary hosting does not establish the original author, licence or a relationship to the UTEFit branch.

During the final read, ID 1's model URL had changed to `https://res.cloudinary.com/tburyrz8/image/upload/v1793295123/utefit/branches/cybv6sdzygkvpzgsqh2g.png` (1448 × 1086, HTTP 200, UTEFIT signage). This task submitted no branch save/upload and did not write to the database; the actor responsible cannot be identified from these GET checks. Signage alone does not verify actual premises or usage rights. Both snapshots are retained in the report; the new source remains unverified and is not published as a facility photo.

`fragments/branch-media.html` now owns the shared safe display policy for Public/Member list/detail and homepage gallery. Homepage no longer requests these unverified branch images; its existing featured IDs remain 5, 4, 3. Admin list/edit retain a clearly labelled unverified source preview and a read-only URL for audit, with an explicit load-error state. No verification field or approval state was invented. No database or original asset was modified. See `BRANCH_IMAGE_SYNC_REPORT.md` at the repository root for the five-ID acceptance table, source URLs, role checks, limitations and before/after screenshots.
