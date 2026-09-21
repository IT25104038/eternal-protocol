// MILESTONE 1 SCOPE — the shared contract between all four modules.
// Mirrors BACKEND_API_SPEC.md. When a backend DTO changes, the owner of that
// DTO opens the pull request against this file — not Gaveen.
// 
// Deferred to Milestone 2: ProductStatus, showPrice, UploadImageResponse,
// ProductImageDto / ProductImageInput, AthleteDashboardDto,
// AthleteDashboardOrder, CommissionDto and CommissionStatus.

export type Role = 'CUSTOMER' | 'ATHLETE' | 'ADMIN'

export type PaymentStatus = 'PENDING' | 'PAID' | 'CANCELLED'

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
}

// ---- Module 1: Storefront & Catalog ----

export type ProductCategory = 'MENS' | 'WOMENS' | 'ACCESSORIES'

export interface ProductDto {
  id: number
  styleCode: string
  name: string
  category: ProductCategory | null
  subCategory: string | null
  price: number
  colour: string
  size: string
  stockQty: number
  imageUrl: string | null
  description: string | null
  /** When this variant was first created — admin-immutable. ISO 8601 string. */
  createdAt: string
}

export interface ProductInput {
  styleCode: string
  name: string
  category: ProductCategory
  subCategory: string
  price: number
  colour: string
  size: string
  stockQty: number
  description?: string
  imageUrl?: string
}

// ---- Module 2: Cart, Checkout & Payment ----

export interface ValidateCodeResponse {
  valid: boolean
  code: string
  discountPercentage: number
}

export interface OrderLineItemDto {
  productId: number
  productName: string
  size: string
  colour: string
  quantity: number
  unitPrice: number
  lineTotal: number
}

export interface OrderItemInput {
  productId: number
  size: string
  colour: string
  quantity: number
}

export interface CreateOrderInput {
  customerName: string
  phone: string
  address: string
  items: OrderItemInput[]
  athleteCode?: string
}

export interface CreateOrderResponse {
  orderId: number
  totalAmount: number
  // MILESTONE 2 adds: paymentUrl: string (the PayHere redirect URL)
}

export interface OrderDetailDto {
  id: number
  items: OrderLineItemDto[]
  subtotalAmount: number
  discountAmount: number
  totalAmount: number
  paymentStatus: PaymentStatus
  athleteCode: string | null
  orderDate: string
}

export interface CustomerProfileDto {
  id: number
  name: string
  email: string
  phone: string
  address: string
}

export interface UpdateProfileInput {
  name: string
  phone: string
  address: string
}

export interface CartItemDto {
  id: number
  productId: number
  productName: string
  price: number
  size: string
  colour: string
  quantity: number
  imageUrl: string | null
}

export interface AddCartItemInput {
  productId: number
  size: string
  colour: string
  quantity: number
}

// ---- Module 3: Athlete System & Admin Dashboard ----

export interface AdminOrderDto {
  id: number
  customerName: string
  customerPhone: string
  customerAddress: string
  items: OrderLineItemDto[]
  subtotalAmount: number
  totalAmount: number
  discountAmount: number
  paymentStatus: PaymentStatus
  athleteCode: string | null
  orderDate: string
}

export interface AthleteDto {
  id: number
  name: string
  email: string
  athleteCode: string
  commissionRate: number
  active: boolean
}

export interface CreateAthleteInput {
  name: string
  email: string
  password: string
  athleteCode: string
  commissionRate: number
}

export interface UpdateAthleteInput {
  commissionRate?: number
  active?: boolean
}

// ---- Auth ----

export interface LoginInput {
  email: string
  password: string
}

export interface RegisterInput {
  name: string
  email: string
  password: string
  phone: string
  address: string
}

export interface AuthResponse {
  token: string
  role: Role
  userId: number
}

// ------------------------------------------------------------------------
// MILESTONE 2 adds, in this file:
//   ProductStatus, ProductDto.status, ProductDto.showPrice
//   UploadImageResponse, ProductImageDto, ProductImageInput
//   CommissionStatus, CommissionDto
//   AthleteDashboardOrder, AthleteDashboardDto
// ------------------------------------------------------------------------
