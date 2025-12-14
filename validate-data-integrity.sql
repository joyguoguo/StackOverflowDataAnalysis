-- ============================================================
-- 数据完整性验证脚本
-- 用于检查数据库中的数据一致性和完整性问题
-- ============================================================

\echo '========================================'
\echo '数据完整性验证报告'
\echo '========================================'
\echo ''

-- ============================================================
-- 1. 检查问题标记为已回答但没有答案
-- ============================================================
\echo '1. 检查问题标记为已回答但没有答案...'
SELECT 
    q.question_id,
    q.title,
    q.answered,
    COUNT(a.answer_id) as actual_answer_count,
    q.answer_count as recorded_answer_count
FROM questions q
LEFT JOIN answers a ON q.question_id = a.question_id
WHERE q.answered = true
GROUP BY q.question_id, q.title, q.answered, q.answer_count
HAVING COUNT(a.answer_id) = 0
ORDER BY q.question_id;

\echo '问题数量: ' || (SELECT COUNT(*) FROM (
    SELECT q.question_id
    FROM questions q
    LEFT JOIN answers a ON q.question_id = a.question_id
    WHERE q.answered = true
    GROUP BY q.question_id
    HAVING COUNT(a.answer_id) = 0
) AS subquery);
\echo ''

-- ============================================================
-- 2. 检查问题的answer_count与实际答案数量不一致
-- ============================================================
\echo '2. 检查answer_count字段与实际答案数量不一致...'
SELECT 
    q.question_id,
    q.title,
    q.answer_count as recorded_count,
    COUNT(a.answer_id) as actual_count,
    (q.answer_count - COUNT(a.answer_id)) as difference
FROM questions q
LEFT JOIN answers a ON q.question_id = a.question_id
GROUP BY q.question_id, q.title, q.answer_count
HAVING q.answer_count != COUNT(a.answer_id)
ORDER BY ABS(q.answer_count - COUNT(a.answer_id)) DESC
LIMIT 20;

\echo '不一致问题数量: ' || (SELECT COUNT(*) FROM (
    SELECT q.question_id
    FROM questions q
    LEFT JOIN answers a ON q.question_id = a.question_id
    GROUP BY q.question_id, q.answer_count
    HAVING q.answer_count != COUNT(a.answer_id)
) AS subquery);
\echo ''

-- ============================================================
-- 3. 检查accepted_answer_id指向不存在的答案
-- ============================================================
\echo '3. 检查accepted_answer_id指向不存在的答案...'
SELECT 
    q.question_id,
    q.title,
    q.accepted_answer_id
FROM questions q
WHERE q.accepted_answer_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM answers a WHERE a.answer_id = q.accepted_answer_id
  )
ORDER BY q.question_id;

\echo '问题数量: ' || (SELECT COUNT(*) FROM questions q
WHERE q.accepted_answer_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM answers a WHERE a.answer_id = q.accepted_answer_id
  ));
\echo ''


-- ============================================================
-- 4. 检查accepted_answer_id指向的答案不属于该问题
-- ============================================================
\echo '4. 检查accepted_answer_id指向的答案不属于该问题...'
SELECT 
    q.question_id,
    q.title,
    q.accepted_answer_id,
    a.question_id as actual_question_id
FROM questions q
INNER JOIN answers a ON q.accepted_answer_id = a.answer_id
WHERE q.question_id != a.question_id
ORDER BY q.question_id;

\echo '问题数量: ' || (SELECT COUNT(*) FROM questions q
INNER JOIN answers a ON q.accepted_answer_id = a.answer_id
WHERE q.question_id != a.question_id);
\echo ''

-- ============================================================
-- 5. 检查accepted_answer_id指向的答案的accepted字段不为true
-- ============================================================
\echo '5. 检查accepted_answer_id指向的答案accepted字段不为true...'
SELECT 
    q.question_id,
    q.title,
    q.accepted_answer_id,
    a.accepted as answer_accepted_flag
FROM questions q
INNER JOIN answers a ON q.accepted_answer_id = a.answer_id
WHERE a.accepted != true OR a.accepted IS NULL
ORDER BY q.question_id;

\echo '问题数量: ' || (SELECT COUNT(*) FROM questions q
INNER JOIN answers a ON q.accepted_answer_id = a.answer_id
WHERE a.accepted != true OR a.accepted IS NULL);
\echo ''

-- ============================================================
-- 6. 检查孤立的question_comments（指向不存在的问题）
-- ============================================================
\echo '6. 检查孤立的question_comments（指向不存在的问题）...'
SELECT 
    qc.comment_id,
    qc.question_id,
    LEFT(qc.body, 50) as body_preview
FROM question_comments qc
LEFT JOIN questions q ON qc.question_id = q.question_id
WHERE q.question_id IS NULL
ORDER BY qc.comment_id;

\echo '孤立评论数量: ' || (SELECT COUNT(*) FROM question_comments qc
LEFT JOIN questions q ON qc.question_id = q.question_id
WHERE q.question_id IS NULL);
\echo ''

-- ============================================================
-- 7. 检查孤立的answer_comments（指向不存在的答案）
-- ============================================================
\echo '7. 检查孤立的answer_comments（指向不存在的答案）...'
SELECT 
    ac.comment_id,
    ac.answer_id,
    LEFT(ac.body, 50) as body_preview
FROM answer_comments ac
LEFT JOIN answers a ON ac.answer_id = a.answer_id
WHERE a.answer_id IS NULL
ORDER BY ac.comment_id;

\echo '孤立评论数量: ' || (SELECT COUNT(*) FROM answer_comments ac
LEFT JOIN answers a ON ac.answer_id = a.answer_id
WHERE a.answer_id IS NULL);
\echo ''

-- ============================================================
-- 8. 检查孤立的answers（指向不存在的问题）
-- ============================================================
\echo '8. 检查孤立的answers（指向不存在的问题）...'
SELECT 
    a.answer_id,
    a.question_id,
    LEFT(a.body, 50) as body_preview
FROM answers a
LEFT JOIN questions q ON a.question_id = q.question_id
WHERE q.question_id IS NULL
ORDER BY a.answer_id;

\echo '孤立回答数量: ' || (SELECT COUNT(*) FROM answers a
LEFT JOIN questions q ON a.question_id = q.question_id
WHERE q.question_id IS NULL);
\echo ''

-- ============================================================
-- 9. 检查questions的owner_account_id指向不存在的用户
-- ============================================================
\echo '9. 检查questions的owner指向不存在的用户...'
SELECT 
    q.question_id,
    q.title,
    q.owner_account_id
FROM questions q
LEFT JOIN users u ON q.owner_account_id = u.account_id
WHERE u.account_id IS NULL
ORDER BY q.question_id;

\echo '问题数量: ' || (SELECT COUNT(*) FROM questions q
LEFT JOIN users u ON q.owner_account_id = u.account_id
WHERE u.account_id IS NULL);
\echo ''

-- ============================================================
-- 10. 检查answers的owner_account_id指向不存在的用户
-- ============================================================
\echo '10. 检查answers的owner指向不存在的用户...'
SELECT 
    a.answer_id,
    a.question_id,
    a.owner_account_id
FROM answers a
LEFT JOIN users u ON a.owner_account_id = u.account_id
WHERE u.account_id IS NULL
ORDER BY a.answer_id;

\echo '回答数量: ' || (SELECT COUNT(*) FROM answers a
LEFT JOIN users u ON a.owner_account_id = u.account_id
WHERE u.account_id IS NULL);
\echo ''

-- ============================================================
-- 11. 检查question_comments的owner_account_id指向不存在的用户
-- ============================================================
\echo '11. 检查question_comments的owner指向不存在的用户...'
SELECT 
    qc.comment_id,
    qc.question_id,
    qc.owner_account_id
FROM question_comments qc
LEFT JOIN users u ON qc.owner_account_id = u.account_id
WHERE u.account_id IS NULL
ORDER BY qc.comment_id;

\echo '评论数量: ' || (SELECT COUNT(*) FROM question_comments qc
LEFT JOIN users u ON qc.owner_account_id = u.account_id
WHERE u.account_id IS NULL);
\echo ''

-- ============================================================
-- 12. 检查answer_comments的owner_account_id指向不存在的用户
-- ============================================================
\echo '12. 检查answer_comments的owner指向不存在的用户...'
SELECT 
    ac.comment_id,
    ac.answer_id,
    ac.owner_account_id
FROM answer_comments ac
LEFT JOIN users u ON ac.owner_account_id = u.account_id
WHERE u.account_id IS NULL
ORDER BY ac.comment_id;

\echo '评论数量: ' || (SELECT COUNT(*) FROM answer_comments ac
LEFT JOIN users u ON ac.owner_account_id = u.account_id
WHERE u.account_id IS NULL);
\echo ''

-- ============================================================
-- 13. 统计摘要
-- ============================================================
\echo '========================================'
\echo '统计摘要'
\echo '========================================'

SELECT 
    'Questions' as table_name, COUNT(*) as total_count 
FROM questions
UNION ALL
SELECT 
    'Answers' as table_name, COUNT(*) as total_count 
FROM answers
UNION ALL
SELECT 
    'Question Comments' as table_name, COUNT(*) as total_count 
FROM question_comments
UNION ALL
SELECT 
    'Answer Comments' as table_name, COUNT(*) as total_count 
FROM answer_comments
UNION ALL
SELECT 
    'Users' as table_name, COUNT(*) as total_count 
FROM users
UNION ALL
SELECT 
    'Tags' as table_name, COUNT(*) as total_count 
FROM tags
ORDER BY table_name;

\echo ''
\echo '========================================'
\echo '验证完成'
\echo '========================================'

