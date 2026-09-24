# docs/review

本目录记录 bgssai-media 的验收结论与功能测试用例，**不等于**产品线全功能通过。规划目标见骨架仓验收基线；已实现/已验证以本目录文档为准。

## 验收记录

| 文档 | 说明 |
| --- | --- |
| [acceptance-20260911.md](./acceptance-20260911.md) | 2026-09-11 需求版本对应的实现与验证状态（权威） |
| [media-02-long-contract-20260916.md](./media-02-long-contract-20260916.md) | MEDIA-02 Long contract + SourceRefs 专项记录 |

## 功能测试用例

目录：[test-cases/](./test-cases/)

| 文件 | 说明 |
| --- | --- |
| [test-cases/README.md](./test-cases/README.md) | 用例索引（功能清单、AC/TC 计数） |
| [test-cases/TEMPLATE.md](./test-cases/TEMPLATE.md) | 用例表模板与编写约定 |
| [test-cases/STATUS.md](./test-cases/STATUS.md) | 生成/执行状态摘要 |
| [test-cases/TC-*.md](./test-cases/) | 按功能 slug 展开的用例表（54 条，in-scope） |

约定：

1. 用例由 `docs/feature/*.md` 验收标准 1:1 展开；OUT-OF-SCOPE 标 N/A。
2. 文档不含口令、JWT、PAT、AK/SK、短信密钥等密钥。
3. 真机 / 真实支付 / 真实三方 OAuth 未就绪项在备注标 **PENDING**，不得记 PASS。
4. 不得把文件存在或单元测试通过写成全功能通过。

## 相关链接

- 产品线愿景：[`docs/PRODUCT-LINE-VISION.md`](../PRODUCT-LINE-VISION.md)
- 跨仓规范：[`docs/BGSSAI-Standards.md`](../BGSSAI-Standards.md)
- 骨架仓验收基线：[product-line-acceptance.md](https://github.com/liuliuzo/bgssai-skeleton/blob/develop/docs/feature/product-line-acceptance.md)
