# Gaveen: Cloudinary image upload (frontend) and setup docs

Brings back the drag-and-drop photo uploader on the admin product page.

## Edited files
- `frontend/src/api/products.ts`: new `productsApi.uploadImage(file, onProgress)`, which posts the file as `multipart/form-data` to `/api/products/upload-image`. The existing `UploadImageResponse` type in `types/index.ts` was already there.
- `frontend/src/features/admin/AdminProductsPage.tsx`: the "Photo URL" box is replaced by the uploader.
  - Click or drag one or more photos onto the drop zone
  - Files are checked before upload (JPEG, PNG, WebP or GIF, max 5MB each), with an error toast per rejected file
  - Photos upload one at a time, with an "Uploading photo 2 of 4…" counter
  - Each uploaded photo is then attached to the style and colour using the returned `imageUrl` and `publicId`, so deleting it later also removes the Cloudinary file
- `README.md`:
  - Adds the three `CLOUDINARY_*` settings to the setup table, with a link to the free sign-up
  - Notes that photos are uploaded from the admin products page
  - Two small fixes found on the way: the `MYSQL_PASSWORD` table row was missing its leading `|`, and the `mvn spring-boot:run` command had an unclosed quote

## Depends on
Rithil's `POST /api/products/upload-image`. You can build the UI first, but uploading only works once his backend is running with real Cloudinary keys.

## Try it
1. Start the backend (with Rithil's changes and Cloudinary keys), then `cd frontend && npm install && npm run dev`.
2. Log in as admin, open `/admin/products`, fill in a style code and colour, then drop two or three photos.
3. Check the counter, that the photos appear in the gallery, that "cover" can be switched, and that a PDF or oversized file is rejected with a toast.
4. Type-check: `cd frontend && npx tsc -b` (passes on this version).

## Note
This is the first repo's uploader restored as it was. If the team wants a "paste a URL" fallback beside it, that is a small addition to `AdminProductsPage.tsx`.
