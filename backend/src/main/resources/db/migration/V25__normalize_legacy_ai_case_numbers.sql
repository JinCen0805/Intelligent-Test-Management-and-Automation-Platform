-- Replace legacy TC-AI identifiers with stable module-based identifiers.
-- Use a temporary value first so the unique case-number constraint cannot
-- collide while rows are being renamed.
UPDATE test_cases
SET case_no = CONCAT('MIGRATING_AI_', id)
WHERE source = 'AI'
  AND case_no LIKE 'TC-AI-%';

CREATE TEMPORARY TABLE ai_case_numbers AS
SELECT
    id,
    CONCAT(
    CASE numbered.module_name
        WHEN '系统门户' THEN 'MENHU'
        WHEN '采购（用户）工作台' THEN 'CAIGOUYONGHU'
        WHEN '采购与预算执行监督管理' THEN 'CAIGOUYUSUAN'
        WHEN '市场调研与论证管理' THEN 'SHICHANGDIAOYU'
        WHEN '采购需求书在线编制' THEN 'CAIGOUXUQIU'
        WHEN '采购实施计划在线编制' THEN 'CAIGOUJIHUA'
        WHEN '采购需求审查' THEN 'CAIGOUXUQIUSHENCHA'
        WHEN '采购方式审批' THEN 'CAIGOUFANGSHI'
        WHEN '采购申请、采购执行申请与审批管理' THEN 'CAIGOUSHENQING'
        WHEN '采购执行管理（采购对外执行平台）' THEN 'CAIGOUZHIXING'
        WHEN '审计管理' THEN 'SHENJI'
        WHEN '定标管理' THEN 'DINGBIAO'
        WHEN '合同与支付管理' THEN 'HETONGZHIFU'
        WHEN '纪检监察管理' THEN 'JIJIAN'
        ELSE 'MODULE'
    END,
    '_',
    LPAD(numbered.sequence_no, 3, '0')
) AS new_case_no
FROM (
    SELECT
        id,
        project_id,
        suite_type,
        module_name,
        ROW_NUMBER() OVER (
            PARTITION BY project_id, suite_type, module_name
            ORDER BY id
        ) AS sequence_no
    FROM test_cases
    WHERE source = 'AI'
      AND case_no LIKE 'MIGRATING_AI_%'
) numbered;

UPDATE test_cases
SET case_no = (
    SELECT new_case_no
    FROM ai_case_numbers
    WHERE ai_case_numbers.id = test_cases.id
)
WHERE source = 'AI'
  AND case_no LIKE 'MIGRATING_AI_%';

DROP TABLE ai_case_numbers;
