package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.QTeam;
import com.example.querydsl.entity.Team;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.example.querydsl.entity.QMember.*;
import static com.example.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory; // 멀티스레드 환경에서 동시성 문제가 없이 설계되어 있다.

    @BeforeEach // 각 테스트 케이스전에 실행한다.
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // member1을 찾아라
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";

        // 첫 번째 인자로 JPQL 문자열(qlString)을 받고, 두 번째 인자로 결과로 받을 엔티티 타입을 지정합니다.
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        // .getSingleResult() 는
        // 실행된 쿼리의 결과를 단일 객체 하나로 반환하는 메서드입니다.
        // 결과가 정확히 1건일 때 사용합니다.
        // 만약 결과가 없으면 NoResultException
        // 2건 이상이면 NonUniqueResultException 예외가 발생합니다.
        // 참고로 여러 건을 가져오려면 .getResultList()를 사용합니다.

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        // 참고로 gradle 사용시 Querydsl 사용하려면 gradle - Task - other - compileQuerydsl 을 실행해주면
        // build 파일에 querydsl 관련 entity가 생성된다.
//        QMember m = new QMember("m");
//        QMember m = QMember.member; // static import 도 가능
        // QMember를 import 해서 member로 바로 사용 가능

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) // 파라미터 바인딩을 자동으로 해준다.
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where( // 이것도 and 조건이 된다.
                        member.username.eq("member1"),
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void resultFetch() {
        // List
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();

        // 단 건
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();

        // 처음 한 건 조회
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .fetchFirst();
//
//        // 페이징에서 사용
//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults(); // 페이징 정보 포함, total count 쿼리 추가 실행
//
//        results.getTotal();
//        List<Member> content = results.getResults();
//
//        //count 쿼리로 변경
        long count = queryFactory
                .selectFrom(member)
                .fetchCount();

        // 결과 출력
        System.out.println("count = " + count);
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 0부터 시작한다.(zero index)
                .limit(2) // 최대 2건 조회
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    /*
    주의: count 쿼리가 실행되니 성능상 주의!
    참고: 실무에서 페이징 쿼리를 작성할 때, 데이터를 조회하는 쿼리는 여러 테이블을 조인해야 하지만,
    count 쿼리는 조인이 필요 없는 경우도 있다.
    그런데 이렇게 자동화된 count 쿼리는 원본 쿼리와 같이 모두 조인을 해버리기 때문에 성능이 안나올 수 있다.
    count 쿼리에 조인이 필요없는 성능 최적화가 필요하다면, count 전용 쿼리를 별 도로 작성해야 한다.
     */
    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 0부터 시작한다.(zero index)
                .limit(2) // 최대 2건 조회
                .fetchResults(); // 페이징 정보 포함, total count 쿼리 추가 실행

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * JPQL
     * select
     * COUNT(m),
     * SUM(m.age),
     * AVG(m.age),
     * MAX(m.age),
     * MIN(m.age)
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        // result 순회해서 출력
//        for( Tuple tuple : result) {
//            System.out.println("~~~tuple = " + tuple);
//        }

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    // **기본 조인**
    // 조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고, 두 번째 파라미터에 별칭(alias)으로 사용할 Q 타입을 지정하면 된다.
    // join(조인 대상, 별칭으로 사용할 Q타입)

    /**
     * 팀 A에 소속된 모든 회원
     **/
    @Test
    public void join() throws Exception {
        QMember member = QMember.member;
        QTeam team = QTeam.team;
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        // `join()` , `innerJoin()` : 내부 조인(inner join)
        // `leftJoin()` : left 외부 조인(left outer join)
        // `rightJoin()` : rigth 외부 조인(rigth outer join)

//        for( Member m : result) {
//            System.out.println("~~~m = " + m);
//        }
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    // **세타 조인**
    // 연관관계가 없는 필드로 조인

    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     **/
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        // from 절에 여러 엔티티를 선택해서 세타 조인
        // 외부조인불가능 다음에 설명할 조인 on을 사용하면 외부조인 가능

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }


    // ON절을 활용한 조인(JPA 2.1부터 지원)
    // 1. 조인 대상 필터링
    // 2. 연관관계 없는 엔티티 외부 조인

    // 1. 조인 대상 필터링
    // 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     * t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        // member에 이미 team이 연관관계로 매핑되어 있어서, 그 연관관계를 활용해 team.name이 'teamA'인 team만 조건으로 걸어서 조인하는 것
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }
    // 참고: on 절을 활용해 조인 대상을 필터링 할 때, 외부조인이 아니라 내부조인(inner join)을 사용하면,
    // where 절 에서 필터링 하는 것과 기능이 동일하다.
    // 따라서 on 절을 활용한 조인 대상 필터링을 사용할 때, 내부조인 이면 익 숙한 where 절로 해결하고,
    // 정말 외부조인이 필요한 경우에만 이 기능을 사용하자.

    // 2. 연관관계 없는 엔티티 외부 조인
    // 예) 회원의 이름과 팀의 이름이 같은 대상 **외부 조인**

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        // member.username과 team.name 사이에 JPA 연관관계가 없다.
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

    // 조인 - 페치 조인
    // 페치 조인은 SQL에서 제공하는 기능은 아니다. SQL조인을 활용해서 연관된 엔티티를 SQL 한번에 조회하는 기능이ㄷㅏ.
    // 주로 성능 최적화에 사용하는 방법이다.

    // **페치 조인 미적용**
    // 지연로딩으로 Member, Team SQL 쿼리 각각 실행
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    // **페치 조인 적용**
    // 즉시로딩으로 Member, Team SQL 쿼리 조인으로 한번에 조회
    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

}