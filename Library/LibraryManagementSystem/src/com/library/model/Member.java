package com.library.model;

public class Member {
    private int memberId;
    private String membershipNo;
    private String fullName;
    private String email;
    private String phone;
    private String status;

    public Member() {}

    public Member(int memberId, String membershipNo, String fullName, String email, String phone, String status) {
        this.memberId = memberId;
        this.membershipNo = membershipNo;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.status = status;
    }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getMembershipNo() { return membershipNo; }
    public void setMembershipNo(String membershipNo) { this.membershipNo = membershipNo; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s | Status: %s",
                membershipNo, fullName, email == null ? "-" : email, phone == null ? "-" : phone, status);
    }
}
