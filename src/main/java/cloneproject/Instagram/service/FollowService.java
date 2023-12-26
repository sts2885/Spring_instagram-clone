package cloneproject.Instagram.service;

import java.util.List;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cloneproject.Instagram.entity.member.Follow;
import cloneproject.Instagram.entity.member.Member;
import cloneproject.Instagram.exception.AlreadyFollowException;
import cloneproject.Instagram.exception.CantUnfollowException;
import cloneproject.Instagram.exception.MemberDoesNotExistException;
import cloneproject.Instagram.repository.FollowRepository;
import cloneproject.Instagram.repository.MemberRepository;
import cloneproject.Instagram.vo.UsernameWithImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;
import static java.util.stream.Collectors.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepository followRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public boolean follow(String followMemberUsername){
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findById(Long.valueOf(memberId))
                                        .orElseThrow(MemberDoesNotExistException::new);
        Member followMember = memberRepository.findByUsername(followMemberUsername)
                                                .orElseThrow(MemberDoesNotExistException::new);
        if(followRepository.existsByMemberIdAndFollowMemberId(member.getId(), followMember.getId())){
            throw new AlreadyFollowException();
        }
        Follow follow = new Follow(member, followMember);
        followRepository.save(follow);
        return true;
    }

    @Transactional
    public boolean unfollow(String followMemberUsername){
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
        Member followMember = memberRepository.findByUsername(followMemberUsername)
                                                .orElseThrow(MemberDoesNotExistException::new);
        Follow follow = followRepository.findByMemberIdAndFollowMemberId(Long.valueOf(memberId), followMember.getId())
                                        .orElseThrow(CantUnfollowException::new);
        followRepository.delete(follow);
        return true;
    }

    @Transactional(readOnly = true)
    public List<UsernameWithImage> getFollowings(String memberUsername){ 
        Member member = memberRepository.findByUsername(memberUsername)
                                                .orElseThrow(MemberDoesNotExistException::new);
        List<Follow> follows = followRepository.findAllByMemberId(member.getId());
        List<Member> followingMembers = follows.stream()
                                                .map(follow->follow.getFollowMember())
                                                .collect(Collectors.toList());
        List<UsernameWithImage> result = followingMembers.stream()
                                                .map(this::convertMemberToUsernameWithImages)
                                                .collect(Collectors.toList());
        return result;
    }

    @Transactional(readOnly = true)
    public List<UsernameWithImage> getFollowers(String memberUsername){ 
        Member member = memberRepository.findByUsername(memberUsername)
                                                .orElseThrow(MemberDoesNotExistException::new);
        List<Follow> follows = followRepository.findAllByFollowMemberId(member.getId());
        List<Member> followingMembers = follows.stream()
                                                .map(follow->follow.getMember())
                                                .collect(Collectors.toList());
        List<UsernameWithImage> result = followingMembers.stream()
                                                .map(this::convertMemberToUsernameWithImages)
                                                .collect(Collectors.toList());
        return result;
    }

    /**
     * member_id(pk)만을 담은 팔로잉 목록
     * 메인화면(포스트목록) 구현 때 사용
     * @return List<Long>: 로그인한 사용자가 팔로우중인 멤버들의 id 목록
     */
    @Transactional(readOnly = true)
    public List<Long> getOnlyFollowingsMemberId(){
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Follow> follows = followRepository.findAllByMemberId(Long.valueOf(memberId));
        List<Long> result = follows.stream()
                                        .map(follow->follow.getFollowMember().getId())
                                        .collect(Collectors.toList());
        return result;

    }

    /**
     * 프로필, 게시물에서 팔로우 여부를 판단하기 위한 메서드
     * @param memberId 멤버ID (현재 로그인 중인 사용자의 PK)
     * @param followMemberUsername 대상의 username
     * @return boolean: 팔로우 여부
     */
    @Transactional(readOnly = true)
    public boolean isFollowing(String followMemberUsername){
        String memberId = SecurityContextHolder.getContext().getAuthentication().getName();
        Member followMember = memberRepository.findByUsername(followMemberUsername)
                                                .orElseThrow(MemberDoesNotExistException::new);
        return followRepository.existsByMemberIdAndFollowMemberId(Long.valueOf(memberId), followMember.getId());
    }

    private UsernameWithImage convertMemberToUsernameWithImages(Member member){
        UsernameWithImage result = UsernameWithImage.builder()
                                                    .username(member.getUsername())
                                                    .image(member.getImage())
                                                    .build();
        return result;
    }

}
