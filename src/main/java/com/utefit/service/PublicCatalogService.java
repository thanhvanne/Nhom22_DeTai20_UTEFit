package com.utefit.service;

import com.utefit.entity.Branch;
import com.utefit.entity.FitnessClass;
import com.utefit.entity.MembershipPlan;
import com.utefit.enums.ClassStatus;
import com.utefit.repository.BranchRepository;
import com.utefit.repository.MembershipPlanRepository;
import com.utefit.repository.PublicFitnessClassRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PublicCatalogService {

    private static final int PAGE_SIZE = 6;
    private static final Set<ClassStatus> PUBLIC_STATUSES =
            Set.of(ClassStatus.OPEN, ClassStatus.FULL);

    private final BranchRepository branches;
    private final MembershipPlanRepository plans;
    private final PublicFitnessClassRepository classes;

    public PublicCatalogService(
            BranchRepository branches,
            MembershipPlanRepository plans,
            PublicFitnessClassRepository classes
    ) {
        this.branches = branches;
        this.plans = plans;
        this.classes = classes;
    }

    @Transactional(readOnly = true)
    public Page<BranchView> searchBranches(
            String keyword, int page
    ) {
        return branches.searchBranches(
                normalize(keyword),
                true,
                pageable(page)
        ).map(this::toBranchView);
    }

    @Transactional(readOnly = true)
    public BranchView branchDetail(Long id) {
        Branch branch = branches.findById(id)
                .filter(b -> Boolean.TRUE.equals(b.getActive()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy chi nhánh"
                ));

        return toBranchView(branch);
    }

    @Transactional(readOnly = true)
    public Page<PlanView> searchPlans(
            String keyword, int page
    ) {
        return plans.searchFixedPlans(
                normalize(keyword),
                pageable(page)
        ).map(this::toPlanView);
    }

    @Transactional(readOnly = true)
    public Page<ClassView> searchClasses(
            String keyword, Long branchId, int page
    ) {
        return classes.searchPublicClasses(
                normalize(keyword),
                branchId,
                PUBLIC_STATUSES,
                pageable(page)
        ).map(this::toClassView);
    }

    @Transactional(readOnly = true)
    public List<BranchView> featuredBranches() {
        return searchBranches(null, 0)
                .getContent()
                .stream()
                .limit(3)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanView> featuredPlans() {
        return searchPlans(null, 0)
                .getContent()
                .stream()
                .limit(3)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClassView> featuredClasses() {
        return searchClasses(null, null, 0)
                .getContent()
                .stream()
                .limit(3)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanView> activePlanOptions() {
        var activeIds = branches.findByActiveTrueOrderByNameAsc().stream().map(Branch::getId).collect(java.util.stream.Collectors.toSet());
        return plans.findByActiveTrueOrderByNameAsc()
                .stream().filter(MembershipPlan::fixedBranch).filter(plan -> activeIds.contains(plan.getBranchIds().iterator().next()))
                .map(this::toPlanView).toList();
    }

    @Transactional(readOnly = true)
    public List<BranchView> activeBranchOptions() {
        return branches.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toBranchView)
                .toList();
    }

    private PageRequest pageable(int page) {
        return PageRequest.of(
                Math.max(0, Math.min(page, 10000)),
                PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "id")
        );
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        String result = keyword.trim()
                .toLowerCase(Locale.ROOT);

        if (result.length() > 100) {
            throw new IllegalArgumentException(
                    "Từ khóa tối đa 100 ký tự"
            );
        }

        return "%" + result + "%";
    }

    private BranchView toBranchView(Branch b) {
        return new BranchView(
                b.getId(),
                b.getName(),
                b.getAddress(),
                b.getPhone(),
                b.getDescription(),
                b.getImageUrl(),
                b.getOpeningTime(),
                b.getClosingTime()
        );
    }

    private PlanView toPlanView(MembershipPlan p) {
        return new PlanView(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getDurationDays(),
                p.getPrice(), p.effectiveBenefit().name(), p.effectiveScope().name(), java.util.Set.copyOf(p.getBranchIds()), p.term().description()
        );
    }

    private ClassView toClassView(FitnessClass c) {
        String trainerName = "Đang cập nhật";

        if (c.getTrainer() != null
                && c.getTrainer().getUser() != null) {
            var user = c.getTrainer().getUser();
            trainerName = user.getLastName()
                    + " " + user.getFirstName();
        }

        return new ClassView(
                c.getId(),
                c.getName(),
                c.getDescription(),
                c.getBranch().getName(),
                c.getCategory().getName(),
                trainerName,
                c.getCapacity(),
                c.getStatus().name()
        );
    }

    public record BranchView(
            Long id,
            String name,
            String address,
            String phone,
            String description,
            String imageUrl,
            LocalTime openingTime,
            LocalTime closingTime
    ) {}

    public record PlanView(
            Long id,
            String name,
            String description,
            Integer durationDays,
            BigDecimal price, String benefit, String accessScope, java.util.Set<Long> branchIds, String durationDescription
    ) {}

    public record ClassView(
            Long id,
            String name,
            String description,
            String branchName,
            String categoryName,
            String trainerName,
            Integer capacity,
            String status
    ) {}
}
