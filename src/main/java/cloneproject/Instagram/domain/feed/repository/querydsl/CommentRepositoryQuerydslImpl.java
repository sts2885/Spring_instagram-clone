package cloneproject.Instagram.domain.feed.repository.querydsl;

import cloneproject.Instagram.domain.feed.dto.CommentDto;
import cloneproject.Instagram.domain.feed.dto.QCommentDto;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static cloneproject.Instagram.domain.feed.entity.QComment.comment;
import static cloneproject.Instagram.domain.feed.entity.QCommentLike.commentLike;
import static cloneproject.Instagram.domain.feed.entity.QRecentComment.*;
import static cloneproject.Instagram.domain.member.entity.QMember.member;

@RequiredArgsConstructor
public class CommentRepositoryQuerydslImpl implements CommentRepositoryQuerydsl {

	private final JPAQueryFactory queryFactory;

	@Override
	public List<CommentDto> findAllRecentCommentDto(Long memberId, List<Long> postIds) {
		return queryFactory
			.select(new QCommentDto(
				recentComment.post.id,
				recentComment.comment.id,
				recentComment.member,
				recentComment.comment.content,
				recentComment.comment.uploadDate,
				recentComment.comment.commentLikes.size(),
				isExistCommentLikeWhereCommentEqAndMemberEq(memberId),
				recentComment.comment.children.size()
			))
			.from(recentComment)
			.innerJoin(recentComment.comment, comment)
			.innerJoin(recentComment.member, member)
			.where(recentComment.post.id.in(postIds))
			.fetch();
	}

	@Override
	public Page<CommentDto> findCommentDtoPage(Long memberId, Long postId, Pageable pageable) {
		final List<CommentDto> commentDtos = queryFactory
			.select(new QCommentDto(
				comment.post.id,
				comment.id,
				comment.member,
				comment.content,
				comment.uploadDate,
				comment.commentLikes.size(),
				isExistCommentLikeWhereCommentEqAndMemberEq(memberId),
				comment.children.size()
			))
			.from(comment)
			.where(comment.post.id.eq(postId).and(comment.parent.id.isNull()))
			.innerJoin(comment.member, member)
			.orderBy(comment.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		final long total = queryFactory
			.selectFrom(comment)
			.where(comment.post.id.eq(postId).and(comment.parent.id.isNull()))
			.fetchCount();

		return new PageImpl<>(commentDtos, pageable, total);
	}

	@Override
	public Page<CommentDto> findReplyDtoPage(Long memberId, Long commentId, Pageable pageable) {
		final List<CommentDto> commentDtos = queryFactory
			.select(new QCommentDto(
				comment.post.id,
				comment.id,
				comment.member,
				comment.content,
				comment.uploadDate,
				comment.commentLikes.size(),
				isExistCommentLikeWhereCommentEqAndMemberEq(memberId),
				comment.children.size()
			))
			.from(comment)
			.where(comment.parent.id.eq(commentId))
			.innerJoin(comment.member, member)
			.orderBy(comment.id.desc())
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.fetch();

		final long total = queryFactory
			.selectFrom(comment)
			.where(comment.parent.id.eq(commentId))
			.fetchCount();

		return new PageImpl<>(commentDtos, pageable, total);
	}

	private BooleanExpression isExistCommentLikeWhereCommentEqAndMemberEq(Long memberId) {
		return JPAExpressions
			.selectFrom(commentLike)
			.where(commentLike.comment.eq(comment).and(commentLike.member.id.eq(memberId)))
			.exists();
	}

}
