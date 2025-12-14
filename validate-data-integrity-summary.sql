-- ============================================================
-- 数据完整性验证摘要脚本（快速检查）
-- 只显示问题统计，不显示详细记录
-- ============================================================

\echo '========================================'
\echo '数据完整性验证摘要'
\echo '========================================'
\echo ''

-- 统计摘要
\echo '数据统计:'
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
\echo '完整性检查结果'
\echo '========================================'
\echo ''

-- 1. 问题标记为已回答但没有答案
SELECT 
    '标记为已回答但没有答案的问题' as issue,
    COUNT(*) as count
FROM questions q
LEFT JOIN answers a ON q.question_id = a.question_id
WHERE q.answered = true
GROUP BY q.question_id
HAVING COUNT(a.answer_id) = 0

UNION ALL

-- 2. answer_count不一致
SELECT 
    'answer_count与实际答案数量不一致的问题' as issue,
    COUNT(*) as count
FROM (
    SELECT q.question_id
    FROM questions q
    LEFT JOIN answers a ON q.question_id = a.question_id
    GROUP BY q.question_id, q.answer_count
    HAVING q.answer_count != COUNT(a.answer_id)
) AS subquery

UNION ALL

-- 3. accepted_answer_id指向不存在的答案
SELECT 
    'accepted_answer_id指向不存在的答案' as issue,
    COUNT(*) as count
FROM questions q
WHERE q.accepted_answer_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM answers a WHERE a.answer_id = q.accepted_answer_id
  )

UNION ALL

-- 4. accepted_answer_id指向的答案不属于该问题
SELECT 
    'accepted_answer_id指向的答案不属于该问题' as issue,
    COUNT(*) as count
FROM questions q
INNER JOIN answers a ON q.accepted_answer_id = a.answer_id
WHERE q.question_id != a.question_id

UNION ALL

-- 5. accepted_answer_id指向的答案accepted不为true
SELECT 
    'accepted_answer_id指向的答案accepted不为true' as issue,
    COUNT(*) as count
FROM questions q
INNER JOIN answers a ON q.accepted_answer_id = a.answer_id
WHERE a.accepted != true OR a.accepted IS NULL

UNION ALL

-- 6. 孤立的question_comments
SELECT 
    '孤立的question_comments' as issue,
    COUNT(*) as count
FROM question_comments qc
LEFT JOIN questions q ON qc.question_id = q.question_id
WHERE q.question_id IS NULL

UNION ALL

-- 7. 孤立的answer_comments
SELECT 
    '孤立的answer_comments' as issue,
    COUNT(*) as count
FROM answer_comments ac
LEFT JOIN answers a ON ac.answer_id = a.answer_id
WHERE a.answer_id IS NULL

UNION ALL

-- 8. 孤立的answers
SELECT 
    '孤立的answers' as issue,
    COUNT(*) as count
FROM answers a
LEFT JOIN questions q ON a.question_id = q.question_id
WHERE q.question_id IS NULL

UNION ALL

-- 9. questions的owner不存在
SELECT 
    'questions的owner不存在' as issue,
    COUNT(*) as count
FROM questions q
LEFT JOIN users u ON q.owner_account_id = u.account_id
WHERE u.account_id IS NULL

UNION ALL

-- 10. answers的owner不存在
SELECT 
    'answers的owner不存在' as issue,
    COUNT(*) as count
FROM answers a
LEFT JOIN users u ON a.owner_account_id = u.account_id
WHERE u.account_id IS NULL

UNION ALL

-- 11. question_comments的owner不存在
SELECT 
    'question_comments的owner不存在' as issue,
    COUNT(*) as count
FROM question_comments qc
LEFT JOIN users u ON qc.owner_account_id = u.account_id
WHERE u.account_id IS NULL

UNION ALL

-- 12. answer_comments的owner不存在
SELECT 
    'answer_comments的owner不存在' as issue,
    COUNT(*) as count
FROM answer_comments ac
LEFT JOIN users u ON ac.owner_account_id = u.account_id
WHERE u.account_id IS NULL;

\echo ''
\echo '========================================'
\echo '验证完成'
\echo '========================================'
\echo '如果所有count都为0，说明数据完整性良好！'

