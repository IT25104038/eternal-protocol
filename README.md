# Eternal Protocol

Eternal Protocol is an online store for athlete-branded merchandise. Customers shop for clothing and accessories, and can check out with an athlete's referral code so that athlete earns a commission on the sale. Store admins manage the products, orders, athletes and commission payouts.

## Features

- Browse Men's, Women's and Accessories, with colours, sizes and photo galleries
- New Drops and Upcoming ("Coming Soon") product pages
- Customer accounts: register, log in, edit profile, view order history
- Cart and checkout with PayHere (sandbox) payments
- Optional athlete referral code at checkout
- Athlete dashboard: referral code, referred orders and commission earned
- Admin tools: manage products and photos, orders, athletes and commission payouts
- Separate customer, athlete and admin logins (JWT)

**Built with:** Java 17 + Spring Boot (backend), React + TypeScript + Vite (frontend), H2 or MySQL (database).

## How to run

**You need:** Java 17, Maven, and Node.js 20.19+ (or 22.12+).

### 1. Backend

```bash
cd backend
cp .env.example .env
```

Open `.env` and fill in:

| Setting | What to put |
|---|---|
| `JWT_SECRET` | Any random text, at least 32 characters |
| `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` | From a free [Cloudinary](https://cloudinary.com/users/register/free) account (Dashboard homepage). Needed to upload product photos, and the backend will not start without them |
| `PAYHERE_MERCHANT_ID` / `PAYHERE_MERCHANT_SECRET` | From a free [PayHere sandbox](https://sandbox.payhere.lk) account |
| `ADMIN_BOOTSTRAP_EMAIL` / `ADMIN_BOOTSTRAP_PASSWORD` | Your first admin login (created on first start) |
| `MYSQL_PASSWORD` | The mysql root password |

Then start it:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
```
The backend runs at http://localhost:8080 


### 2. Frontend

In a second terminal:

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Open http://localhost:5173.

### 3. First steps

No sample products are included. Log in at `/login` with your admin email and password, then add products at `/admin/products`. Product photos are uploaded straight from that page (JPEG, PNG, WebP or GIF, up to 5MB each) and stored on Cloudinary.
