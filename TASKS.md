# Methum: Coming Soon behaviour, and the athlete code form

## Part 1: Coming Soon products can't be added to a cart

### Edited files
- `service/CartService.java`: `addToCart` now throws `ProductNotAvailableException` when the product's status is `COMING_SOON`, with the message `"<name>" hasn't launched yet and can't be added to your cart.` The existing exception handler already turns this into a **409**. `OrderService` already blocks these at checkout, so cart and checkout now agree.
- `test/.../CartTest.java`: 2 new tests. A coming-soon product is refused and nothing is saved, and an ACTIVE product still works.
- `frontend/src/features/cart/mergeGuestCart.ts`: needed because of the new 409. When a guest logs in, their saved cart is merged line by line. Before, one rejected line made `Promise.all` fail, and the whole merge stopped and nothing was cleared. Now every line is tried on its own:
  - synced lines are removed from the guest cart
  - a line the server refuses with 409 (a product that is still coming soon) is dropped, since it can never sync
  - lines that fail for other reasons, like a network error, stay in the guest cart so nothing is silently lost
  - if any line didn't make it, the function throws afterwards, so the existing "some cart items may not have synced" toast on the login and register pages still appears

## Part 2: athlete code field on the admin form

### Edited files
- `frontend/src/features/admin/AdminAthletesPage.tsx`:
  - the field is now "Athlete code (optional)" with the placeholder "Leave blank to auto-generate, e.g. USA472"
  - the "Required" check is gone
  - an empty value is sent as `undefined`, so the backend generates the code
  - the success toast now reads "Athlete created. Their code is USA472", so the admin sees what was generated
- `frontend/src/types/index.ts`: `CreateAthleteInput.athleteCode` is now optional.

## Depends on
Part 2 needs Chamod's backend. Until it is merged the server still rejects a missing code with a 400.

## Try it
1. `cd backend && mvn test -Dtest=CartTest`
2. Coming Soon: in `/admin/products`, set a product's status to "Coming soon", then try `POST /api/customers/me/cart` for it as a customer. Expect a 409 with the message above.
3. Guest merge: as a guest, add an active product to the cart, then have an admin switch it to Coming soon, then log in. The login should succeed, the toast should appear, and the cart page should not contain that line.
4. Athlete form: `/admin/athletes`, create an athlete with the code left blank, and check the toast and the table.
5. Type-check: `cd frontend && npx tsc -b` (passes on this version).
