# Package Guide

Tai lieu nay mo ta cach cac package trong source phoi hop voi nhau.

## So do tong quan

- common: code dung chung cho toan he thong.
- infrastructure: ket noi voi he thong ben ngoai.
- features: code theo nghiep vu, moi feature la mot module nho.

## Luong xu ly request

1. Client goi endpoint trong feature controller.
2. Controller goi service de xu ly nghiep vu.
3. Service co the goi persistence, storage, security trong infrastructure.
4. Service tra response DTO, controller tra ve HTTP response.
5. Loi phat sinh duoc xu ly boi common exception.

## Nguyen tac

- Business logic dat o service.
- Package common khong phu thuoc vao feature cu the.
- Package infrastructure cung cap adapter, khong chua business rule.
- Moi feature tu dong du va de test doc lap.
