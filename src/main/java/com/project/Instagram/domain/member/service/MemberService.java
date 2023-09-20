package com.project.Instagram.domain.member.service;

import com.project.Instagram.domain.member.dto.SignUpRequest;
import com.project.Instagram.domain.member.dto.UpdatePasswordRequest;
import com.project.Instagram.domain.member.entity.Member;
import com.project.Instagram.domain.member.repository.MemberRepository;
import com.project.Instagram.global.error.BusinessException;
import com.project.Instagram.global.error.ErrorCode;
import com.project.Instagram.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityExistsException;

import static com.project.Instagram.domain.member.entity.Gender.MALE;
import static com.project.Instagram.domain.member.entity.MemberRole.ROLE_USER;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final SecurityUtil securityUtil;
    private final RefreshTokenService refreshTokenService;
    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final EmailAuthService emailAuthService;

    @Transactional
    public boolean signUp(SignUpRequest signUpRequest) {
        if (memberRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new EntityExistsException("해당 사용자 이름이 이미 존재합니다.");
        }

        final String username = signUpRequest.getUsername();

        if (!emailAuthService.checkSignUpCode(username, signUpRequest.getEmail(), signUpRequest.getCode())) {
            return false;
        }

        final Member member = convertRegisterRequestToMember(signUpRequest);
        final String encryptedPassword = bCryptPasswordEncoder.encode(member.getPassword());
        member.setEncryptedPassword(encryptedPassword);
        memberRepository.save(member);

        return true;
    }

    @Transactional
    public void updatePassword(UpdatePasswordRequest updatePasswordRequest){
//        //로그인 로직(추후 로그인 구현후 쓰임)
        Member member = new Member(1L, "yapped",ROLE_USER,"zzzzz3zzzzkkkk412",
                "엽엽이","www.naver.com","안녕하세요","dlduq29@gmail.com","010-2222-2222",MALE);


        member.setEncryptedPassword(bCryptPasswordEncoder.encode(member.getPassword()));
        if(!bCryptPasswordEncoder.matches(updatePasswordRequest.getOldPassword(),member.getPassword())){//요청 비밀번호 현재 비밀번호 매치 확인
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        if(updatePasswordRequest.getNewPassword().equals(updatePasswordRequest.getOldPassword())){
            throw new BusinessException(ErrorCode.PASSWORD_SAME);
        }
        final String password = bCryptPasswordEncoder.encode(updatePasswordRequest.getNewPassword());
        member.setEncryptedPassword(password);
        memberRepository.save(member);
    }

    public void sendAuthEmail(String username, String email) {
        if (memberRepository.existsByUsername(username)) {
            throw new EntityExistsException("해당 사용자 이름이 이미 존재합니다.");
        }
        emailAuthService.sendSignUpCode(username, email);
    }

    private Member convertRegisterRequestToMember(SignUpRequest signUpRequest) {
        return Member.builder()
                .username(signUpRequest.getUsername())
                .name(signUpRequest.getName())
                .password(signUpRequest.getPassword())
                .email(signUpRequest.getEmail())
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.deleteRefreshTokenByValue(securityUtil.getLoginMemberId(), refreshToken);
    }
}
