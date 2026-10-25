# Kiểm kê source Giai đoạn 7

So với baseline `6833d35`, chỉ nhánh `feat/giai-doan-7`. A = tạo mới, M = sửa. Không bao gồm dữ liệu QA/secret hay bằng chứng local bị loại khỏi commit.

| Loại | File | Mục đích |
|---|---|---|
| A | `src/test/java/com/utefit/service/VnpayConcurrencyIntegrationTest.java` | IPN đồng thời với hai transaction và persistence context đã giữ dữ liệu cũ; activation/thông báo đúng một lần |
| A | `.env.gd7.example` | VNPay / tích hợp Membership và CASH |
| A | `GIAI_DOAN_7.md` | Báo cáo, kiểm kê và hướng dẫn demo/khôi phục |
| A | `database/migrations/V701__vnpay.sql` | VNPay / tích hợp Membership và CASH |
| A | `database/migrations/V702__qr_checkin.sql` | QR một lần / thời hạn / check-in dùng chung |
| A | `database/migrations/V703__favorites.sql` | Favorites riêng Member, duplicate và ownership |
| A | `database/migrations/V704__review_video.sql` | Video trên Review / media lifecycle / validation |
| A | `database/migrations/V705__manager_scope.sql` | Manager / branch scope / RBAC / migration |
| M | `pom.xml` | ZXing và dependency kiểm thử security |
| A | `scripts/run-qa.ps1` | Môi trường QA cô lập, không dùng SQL chính khi test |
| A | `src/main/java/com/utefit/config/FeatureClockConfig.java` | QR một lần / thời hạn / check-in dùng chung |
| A | `src/main/java/com/utefit/config/QaDataConfig.java` | Môi trường QA cô lập, không dùng SQL chính khi test |
| A | `src/main/java/com/utefit/config/ReviewMultipartConfig.java` | Video trên Review / media lifecycle / validation |
| M | `src/main/java/com/utefit/config/SchedulingConfig.java` | Môi trường QA cô lập, không dùng SQL chính khi test |
| M | `src/main/java/com/utefit/config/SecurityConfig.java` | Callback VNPay và RBAC Manager, giữ CSRF |
| A | `src/main/java/com/utefit/config/VnpayProperties.java` | VNPay / tích hợp Membership và CASH |
| M | `src/main/java/com/utefit/controller/admin/AdminCheckinController.java` | Admin quét QR bên cạnh check-in thủ công |
| A | `src/main/java/com/utefit/controller/admin/AdminManagerController.java` | Manager / branch scope / RBAC / migration |
| M | `src/main/java/com/utefit/controller/auth/AuthController.java` | Manager / branch scope / RBAC / migration |
| A | `src/main/java/com/utefit/controller/manager/ManagerController.java` | Manager / branch scope / RBAC / migration |
| A | `src/main/java/com/utefit/controller/member/FavoriteCatalogAdvice.java` | Favorites riêng Member, duplicate và ownership |
| A | `src/main/java/com/utefit/controller/member/MemberFavoriteController.java` | Favorites riêng Member, duplicate và ownership |
| M | `src/main/java/com/utefit/controller/member/MemberMembershipController.java` | VNPay / tích hợp Membership và CASH |
| A | `src/main/java/com/utefit/controller/member/MemberQrCheckinController.java` | QR một lần / thời hạn / check-in dùng chung |
| M | `src/main/java/com/utefit/controller/member/MemberReviewController.java` | Video trên Review / media lifecycle / validation |
| A | `src/main/java/com/utefit/controller/member/ReviewUploadAdvice.java` | Video trên Review / media lifecycle / validation |
| A | `src/main/java/com/utefit/controller/member/VnpayController.java` | VNPay / tích hợp Membership và CASH |
| M | `src/main/java/com/utefit/dto/member/ReviewForm.java` | Video trên Review / media lifecycle / validation |
| A | `src/main/java/com/utefit/entity/Favorite.java` | Favorites riêng Member, duplicate và ownership |
| A | `src/main/java/com/utefit/entity/ManagerBranchAssignment.java` | Manager / branch scope / RBAC / migration |
| M | `src/main/java/com/utefit/entity/Payment.java` | VNPay / tích hợp Membership và CASH |
| A | `src/main/java/com/utefit/entity/QrCheckinToken.java` | QR một lần / thời hạn / check-in dùng chung |
| M | `src/main/java/com/utefit/entity/Review.java` | Video trên Review / media lifecycle / validation |
| A | `src/main/java/com/utefit/enums/FavoriteType.java` | Favorites riêng Member, duplicate và ownership |
| M | `src/main/java/com/utefit/enums/UserRole.java` | Manager / branch scope / RBAC / migration |
| A | `src/main/java/com/utefit/repository/FavoriteRepository.java` | Favorites riêng Member, duplicate và ownership |
| A | `src/main/java/com/utefit/repository/ManagerBranchAssignmentRepository.java` | Manager / branch scope / RBAC / migration |
| M | `src/main/java/com/utefit/repository/PaymentRepository.java` | VNPay / tích hợp Membership và CASH |
| A | `src/main/java/com/utefit/repository/QrCheckinTokenRepository.java` | QR một lần / thời hạn / check-in dùng chung |
| M | `src/main/java/com/utefit/service/AdminReviewService.java` | Video trên Review / media lifecycle / validation |
| M | `src/main/java/com/utefit/service/CloudinaryService.java` | Video trên Review / media lifecycle / validation |
| A | `src/main/java/com/utefit/service/FavoriteService.java` | Favorites riêng Member, duplicate và ownership |
| A | `src/main/java/com/utefit/service/ManagerAccessService.java` | Manager / branch scope / RBAC / migration |
| A | `src/main/java/com/utefit/service/ManagerOperationsService.java` | Manager / branch scope / RBAC / migration |
| M | `src/main/java/com/utefit/service/MemberReviewService.java` | Video trên Review / media lifecycle / validation |
| M | `src/main/java/com/utefit/service/MembershipPaymentService.java` | VNPay / tích hợp Membership và CASH |
| M | `src/main/java/com/utefit/service/PublicReviewService.java` | Video trên Review / media lifecycle / validation |
| A | `src/main/java/com/utefit/service/QrCheckinService.java` | QR một lần / thời hạn / check-in dùng chung |
| A | `src/main/java/com/utefit/service/QrImageService.java` | QR một lần / thời hạn / check-in dùng chung |
| A | `src/main/java/com/utefit/service/VnpayGateway.java` | VNPay / tích hợp Membership và CASH |
| A | `src/main/java/com/utefit/service/VnpayPaymentService.java` | VNPay / tích hợp Membership và CASH |
| M | `src/main/java/com/utefit/service/impl/CloudinaryServiceImpl.java` | Video trên Review / media lifecycle / validation |
| A | `src/main/java/com/utefit/validation/VideoValidator.java` | Video trên Review / media lifecycle / validation |
| A | `src/main/resources/application-qa.properties` | Môi trường QA cô lập, không dùng SQL chính khi test |
| A | `src/main/resources/static/js/qr-member.js` | QR một lần / thời hạn / check-in dùng chung |
| A | `src/main/resources/static/js/qr-scanner.js` | QR một lần / thời hạn / check-in dùng chung |
| M | `src/main/resources/templates/admin/checkins/list.html` | Admin quét QR bên cạnh check-in thủ công |
| M | `src/main/resources/templates/admin/fragments/layout.html` | Menu QR/Favorites/Manager, giữ layout hiện có |
| A | `src/main/resources/templates/admin/managers.html` | Manager / branch scope / RBAC / migration |
| M | `src/main/resources/templates/admin/reviews/list.html` | Video trên Review / media lifecycle / validation |
| A | `src/main/resources/templates/fragments/favorite.html` | Favorites riêng Member, duplicate và ownership |
| M | `src/main/resources/templates/fragments/header.html` | Menu QR/Favorites/Manager, giữ layout hiện có |
| M | `src/main/resources/templates/guest/branch-detail.html` | Nút Favorites trên catalog hiện có |
| M | `src/main/resources/templates/guest/branches.html` | Nút Favorites trên catalog hiện có |
| M | `src/main/resources/templates/guest/classes.html` | Nút Favorites trên catalog hiện có |
| M | `src/main/resources/templates/guest/home.html` | Video Review công khai theo moderation hiện có |
| M | `src/main/resources/templates/guest/reviews.html` | Video trên Review / media lifecycle / validation |
| M | `src/main/resources/templates/guest/trainer-detail.html` | Nút Favorites trên catalog hiện có |
| M | `src/main/resources/templates/guest/trainers.html` | Nút Favorites trên catalog hiện có |
| A | `src/main/resources/templates/manager/index.html` | Manager / branch scope / RBAC / migration |
| A | `src/main/resources/templates/manager/schedule-form.html` | Manager / branch scope / RBAC / migration |
| A | `src/main/resources/templates/member/favorites.html` | Favorites riêng Member, duplicate và ownership |
| M | `src/main/resources/templates/member/memberships.html` | VNPay / tích hợp Membership và CASH |
| A | `src/main/resources/templates/member/qr-checkin.html` | QR một lần / thời hạn / check-in dùng chung |
| M | `src/main/resources/templates/member/review-form.html` | Video trên Review / media lifecycle / validation |
| M | `src/main/resources/templates/member/reviews.html` | Video trên Review / media lifecycle / validation |
| A | `src/main/resources/templates/member/vnpay-result.html` | VNPay / tích hợp Membership và CASH |
| M | `src/test/java/com/utefit/cloudinary/CloudinaryTest.java` | Video trên Review / media lifecycle / validation |
| A | `src/test/java/com/utefit/service/CashPaymentRegressionTest.java` | Tích hợp Stage 7 |
| A | `src/test/java/com/utefit/service/CloudinaryVideoTest.java` | Video trên Review / media lifecycle / validation |
| A | `src/test/java/com/utefit/service/FavoriteIntegrationTest.java` | Favorites riêng Member, duplicate và ownership |
| A | `src/test/java/com/utefit/service/ManagerIntegrationTest.java` | Manager / branch scope / RBAC / migration |
| A | `src/test/java/com/utefit/service/QrCheckinIntegrationTest.java` | QR một lần / thời hạn / check-in dùng chung |
| A | `src/test/java/com/utefit/service/ReviewVideoIntegrationTest.java` | Video trên Review / media lifecycle / validation |
| A | `src/test/java/com/utefit/service/VnpayPaymentServiceTest.java` | VNPay / tích hợp Membership và CASH |
| A | `src/test/java/com/utefit/validation/VideoValidatorTest.java` | Video trên Review / media lifecycle / validation |
| A | `src/test/resources/application.properties` | Môi trường QA cô lập, không dùng SQL chính khi test |
| A | `src/main/resources/templates/error/413.html` | Video trên Review / media lifecycle / validation |
| A | `GIAI_DOAN_7_FILES.md` | Báo cáo, kiểm kê và hướng dẫn demo/khôi phục |

Các business rule, migration, test và demo được mô tả trong `GIAI_DOAN_7.md`. `.ai/`, `.playwright-mcp/`, `docs/`, `target/`, cấu hình secret không thuộc checkpoint source.
