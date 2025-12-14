-- 验证数据导入结果
-- 在PostgreSQL中运行此SQL文件来检查导入的数据

SELECT 'Questions' as table_name, COUNT(*) as count FROM questions
UNION ALL
SELECT 'Answers' as table_name, COUNT(*) as count FROM answers
UNION ALL

SELECT 'Question Comments' as table_name, COUNT(*) as count FROM question_comments
UNION ALL
SELECT 'Answer Comments' as table_name, COUNT(*) as count FROM answer_comments
UNION ALL
SELECT 'Users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 'Tags' as table_name, COUNT(*) as count FROM tags
ORDER BY table_name;

-- 检查一些示例数据
SELECT 'Sample Questions' as info;
SELECT question_id, title, answer_count, score FROM questions LIMIT 5;

SELECT 'Sample Question Comments' as info;
SELECT comment_id, question_id, LEFT(body, 50) as body_preview FROM question_comments LIMIT 5;

SELECT 'Sample Answer Comments' as info;
SELECT comment_id, answer_id, LEFT(body, 50) as body_preview FROM answer_comments LIMIT 5;

