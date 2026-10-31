# UTEFit cinematic prototype — asset provenance

The barbell and kettlebell are AI-generated, unbranded photoreal product artwork created for this prototype on 2026-10-29 with OpenAI image generation. They are not documentary photographs of UTEFit equipment, a stock-library purchase, CC0 assets, or imported GLTF models. No image from the TikTok reference or third-party product website was used.

## Files

- `cinema-barbell-v2.webp`: 1280 × 853, transparent alpha; chrome Olympic shaft, graphite plates, copper collars.
- `cinema-barbell-v2-mobile.webp`: 640 × 427, same artwork, alpha retained.
- `cinema-kettlebell-v2.webp`: 896 × 1120, transparent alpha; graphite cast-steel body and brushed-metal handle.
- `cinema-kettlebell-v2-mobile.webp`: 512 × 640, same artwork, alpha retained.

PNG masters and generation/alpha evidence are retained locally in `target/public-cinematic-v2/assets/`. Deployment variants are resized and encoded to WebP only; no vector replacement, traced silhouette, repainting or borrowed logo. A `<picture>` selects the small-screen image so mobile does not download the desktop pair.

## Usage basis

Generated output is supplied under the OpenAI terms applicable to the account, not a separate third-party photo license. The published Terms of Use and Services Agreement describe output ownership between the customer/user and OpenAI, subject to applicable law and the agreement. This note does not assert exclusive copyright, uniqueness, trademark clearance, or CC0 status. Do not redistribute these as photographs of real UTEFit facilities or real branded products.

- https://openai.com/policies/terms-of-use/ (Content / ownership and similarity)
- https://openai.com/policies/services-agreement/ (Customer content)

## Implementation

The same image textures provide the complete endpoint surfaces and each moving fragment's colour. Shared rank textures control local erosion/reconstruction and particle timing. At the initial/final state the shader submits only a complete image surface, with no particle draw call. Reduced-motion/no-JS/no-WebGL states use these same complete images, not the previous SVG posters. All assets are local; no remote image service is called by visitors.
